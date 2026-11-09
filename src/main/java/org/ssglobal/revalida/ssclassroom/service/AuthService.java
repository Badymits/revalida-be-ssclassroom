package org.ssglobal.revalida.ssclassroom.service;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.encrypt.BytesEncryptor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.ssglobal.revalida.ssclassroom.dto.Auth.CohortOptionDto;
import org.ssglobal.revalida.ssclassroom.dto.Auth.LoginRequestDto;
import org.ssglobal.revalida.ssclassroom.dto.Auth.LoginResponseDto;
import org.ssglobal.revalida.ssclassroom.dto.Auth.MeResponseDto;
import org.ssglobal.revalida.ssclassroom.dto.Auth.RegisterRequestDto;
import org.ssglobal.revalida.ssclassroom.enums.Role;
import org.ssglobal.revalida.ssclassroom.exceptions.BadRequestException;
import org.ssglobal.revalida.ssclassroom.exceptions.ConflictException;
import org.ssglobal.revalida.ssclassroom.exceptions.NotFoundException;
import org.ssglobal.revalida.ssclassroom.model.AppUserModel;
import org.ssglobal.revalida.ssclassroom.model.CohortMemberModel;
import org.ssglobal.revalida.ssclassroom.repository.AppUserRepository;
import org.ssglobal.revalida.ssclassroom.repository.CohortMemberRepository;
import org.ssglobal.revalida.ssclassroom.repository.CohortRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AppUserRepository appUserRepository;
    private final CohortRepository cohortRepository;
    private final CohortMemberRepository cohortMemberRepository;
    private final PasswordEncoder passwordEncoder;
    private final BytesEncryptor nameEncryptor;
    private final AuthenticationManager authenticationManager;
    private final JwtEncoder jwtEncoder;

    @Value("${app.jwt.expiration-hours:8}")
    private long expirationHours;

    /** Spring checks the password (UserDetailsService + BCrypt); a wrong login is a BadCredentialsException (401). */
    @Transactional(readOnly = true)
    public LoginResponseDto login(LoginRequestDto request) {
        String username = request.username().trim();
        authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(username, request.password()));

        AppUserModel user = appUserRepository.findByUsername(username)
                .orElseThrow(() -> new BadCredentialsException("Invalid username or password"));
        return issueToken(user);
    }

    /**
     * Self-registration, atomic.
     * learner -> user + exactly one cohort_member row (cohortId required, must exist).
     * trainer -> user only; no cohort_member row, cohortId ignored.
     */
    @Transactional
    public MeResponseDto register(RegisterRequestDto request) {
        Role role = request.role();
        String cohortId = request.cohortId() == null ? "" : request.cohortId().trim();
        String username = request.username().trim();

        if (role == Role.learner) {
            if (cohortId.isEmpty()) {
                throw new BadRequestException("cohortId", "cohortId is required for learners");
            }
            if (!cohortRepository.existsById(cohortId)) {
                throw NotFoundException.of("Cohort", cohortId);
            }
        }
        if (appUserRepository.existsByUsername(username)) {
            throw new ConflictException("Username is already taken");
        }

        AppUserModel user = AppUserModel.builder()
                // names are stored encrypted and never decrypted by this module; the password is hashed (one-way)
                .firstName(encryptName(request.firstName().trim()))
                .lastName(encryptName(request.lastName().trim()))
                .username(username)
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(role)
                .build();
        try {
            user = appUserRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException e) {
            // lost a race on the unique username
            throw new ConflictException("Username is already taken");
        }

        if (role == Role.learner) {
            cohortMemberRepository.save(new CohortMemberModel(cohortId, user.getUserId()));
        }
        return me(user);
    }

    /** @param userId the JWT subject */
    @Transactional(readOnly = true)
    public MeResponseDto me(String userId) {
        // a valid token for a user that no longer exists is treated as unauthenticated
        return me(appUserRepository.findById(userId)
                .orElseThrow(() -> new BadCredentialsException("User no longer exists")));
    }

    /** Public list for the registration dropdown. */
    @Transactional(readOnly = true)
    public List<CohortOptionDto> listCohorts() {
        return cohortRepository.findAll(Sort.by("name")).stream().map(CohortOptionDto::from).toList();
    }

    // trainers have no cohort_member rows, so their cohortIds is []
    private MeResponseDto me(AppUserModel user) {
        List<String> cohortIds = user.getRole() == Role.learner
                ? cohortMemberRepository.findByUserId(user.getUserId()).stream()
                        .map(CohortMemberModel::getCohortId).toList()
                : List.of();
        return MeResponseDto.of(user, cohortIds);
    }

    /** AES-GCM encrypts the name (random IV each time) and stores it as Base64; nothing in this module decrypts it. */
    private String encryptName(String name) {
        return Base64.getEncoder().encodeToString(nameEncryptor.encrypt(name.getBytes(UTF_8)));
    }

    private LoginResponseDto issueToken(AppUserModel user) {
        Duration ttl = Duration.ofHours(expirationHours);
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(user.getUserId())
                .issuedAt(now)
                .expiresAt(now.plus(ttl))
                .claim("role", user.getRole().name())
                .build();
        String token = jwtEncoder.encode(
                JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
        return LoginResponseDto.bearer(token, ttl.toSeconds());
    }
}
