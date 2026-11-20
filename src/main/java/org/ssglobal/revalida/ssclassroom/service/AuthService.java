package org.ssglobal.revalida.ssclassroom.service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
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
    private final AuthenticationManager authenticationManager;
    private final JwtEncoder jwtEncoder;

    @Value("${app.jwt.expiration-hours:8}")
    private long expirationHours;

    @Transactional(readOnly = true)
    public LoginResponseDto login(LoginRequestDto request) {
        String username = request.username().trim();
        authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(username, request.password()));

        AppUserModel user = appUserRepository.findByUsername(username)
                .orElseThrow(() -> new BadCredentialsException("Invalid username or password"));
        return issueToken(user);
    }

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
                .firstName(request.firstName().trim())
                .lastName(request.lastName().trim())
                .username(username)
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(role)
                .build();
        try {
            user = appUserRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("Username is already taken");
        }

        if (role == Role.learner) {
            cohortMemberRepository.save(new CohortMemberModel(cohortId, user.getUserId()));
        }
        return me(user);
    }

    @Transactional(readOnly = true)
    public MeResponseDto me(String userId) {
        return me(appUserRepository.findById(userId)
                .orElseThrow(() -> new BadCredentialsException("User no longer exists")));
    }

    @Transactional(readOnly = true)
    public List<CohortOptionDto> listCohorts() {
        return cohortRepository.findAll(Sort.by("name")).stream().map(CohortOptionDto::from).toList();
    }

    private MeResponseDto me(AppUserModel user) {
        List<String> cohortIds = user.getRole() == Role.learner
                ? cohortMemberRepository.findByUserId(user.getUserId()).stream()
                        .map(CohortMemberModel::getCohortId).toList()
                : List.of();
        return MeResponseDto.of(user, cohortIds);
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
