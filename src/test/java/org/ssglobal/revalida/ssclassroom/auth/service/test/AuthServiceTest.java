package org.ssglobal.revalida.ssclassroom.auth.service.test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.util.ReflectionTestUtils;
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
import org.ssglobal.revalida.ssclassroom.model.CohortModel;
import org.ssglobal.revalida.ssclassroom.repository.AppUserRepository;
import org.ssglobal.revalida.ssclassroom.repository.CohortMemberRepository;
import org.ssglobal.revalida.ssclassroom.repository.CohortRepository;
import org.ssglobal.revalida.ssclassroom.service.AuthService;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final String COHORT = "crs_SEED_BACKEND_Q4";

    @Mock AppUserRepository appUserRepository;
    @Mock CohortRepository cohortRepository;
    @Mock CohortMemberRepository cohortMemberRepository;
    @Mock AuthenticationManager authenticationManager;
    @Mock JwtEncoder jwtEncoder;

    // real encoder: we want to see that the stored value really is a BCrypt hash
    PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(appUserRepository, cohortRepository, cohortMemberRepository,
                passwordEncoder, authenticationManager, jwtEncoder);
        ReflectionTestUtils.setField(authService, "expirationHours", 8L);
    }

    private static RegisterRequestDto learner(String username, String cohortId) {
        return new RegisterRequestDto("Ada", "Lovelace", username, "Password123!", Role.learner, cohortId);
    }

    private static RegisterRequestDto trainer(String username, String cohortId) {
        return new RegisterRequestDto("Tom", "Trainer", username, "Password123!", Role.trainer, cohortId);
    }

    private void stubSaveAssignsId(String id) {
        when(appUserRepository.saveAndFlush(any(AppUserModel.class))).thenAnswer(inv -> {
            AppUserModel u = inv.getArgument(0);
            u.setUserId(id);
            return u;
        });
    }

    // ---------- learner registration ----------

    @Test
    void registerLearner_createsUserAndSingleCohortMembership() {
        when(cohortRepository.existsById(COHORT)).thenReturn(true);
        stubSaveAssignsId("usr_L1");
        when(cohortMemberRepository.findByUserId("usr_L1")).thenReturn(List.of(new CohortMemberModel(COHORT, "usr_L1")));

        MeResponseDto result = authService.register(learner("ada", COHORT));

        ArgumentCaptor<AppUserModel> saved = ArgumentCaptor.forClass(AppUserModel.class);
        verify(appUserRepository).saveAndFlush(saved.capture());
        assertThat(saved.getValue().getRole()).isEqualTo(Role.learner);

        ArgumentCaptor<CohortMemberModel> member = ArgumentCaptor.forClass(CohortMemberModel.class);
        verify(cohortMemberRepository).save(member.capture());
        assertThat(member.getValue().getCohortId()).isEqualTo(COHORT);
        assertThat(member.getValue().getUserId()).isEqualTo("usr_L1");

        assertThat(result.id()).isEqualTo("usr_L1");
        assertThat(result.role()).isEqualTo(Role.learner);
        assertThat(result.displayName()).isEqualTo("Ada Lovelace");
        assertThat(result.cohortIds()).containsExactly(COHORT);
    }

    @Test
    void registerLearner_storesBcryptHashNotPlainPassword() {
        when(cohortRepository.existsById(COHORT)).thenReturn(true);
        stubSaveAssignsId("usr_L1");

        authService.register(learner("ada", COHORT));

        ArgumentCaptor<AppUserModel> saved = ArgumentCaptor.forClass(AppUserModel.class);
        verify(appUserRepository).saveAndFlush(saved.capture());
        String hash = saved.getValue().getPasswordHash();
        assertThat(hash).isNotEqualTo("Password123!").startsWith("$2");
        assertThat(passwordEncoder.matches("Password123!", hash)).isTrue();
    }

    @Test
    void registerLearner_trimsUsernameNamesAndCohortId() {
        when(cohortRepository.existsById(COHORT)).thenReturn(true);
        stubSaveAssignsId("usr_L1");

        authService.register(new RegisterRequestDto("  Ada ", " Lovelace ", "  ada  ", "Password123!",
                Role.learner, " " + COHORT + " "));

        ArgumentCaptor<AppUserModel> saved = ArgumentCaptor.forClass(AppUserModel.class);
        verify(appUserRepository).saveAndFlush(saved.capture());
        assertThat(saved.getValue().getUsername()).isEqualTo("ada");
        assertThat(saved.getValue().getFirstName()).isEqualTo("Ada");
        verify(cohortRepository).existsById(COHORT);
    }

    @Test
    void registerLearner_withoutCohortId_isBadRequestAndNothingIsSaved() {
        assertThatThrownBy(() -> authService.register(learner("ada", null))).isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> authService.register(learner("ada", "   "))).isInstanceOf(BadRequestException.class);

        verifyNoInteractions(appUserRepository, cohortMemberRepository);
    }

    @Test
    void registerLearner_unknownCohort_isNotFoundAndNothingIsSaved() {
        when(cohortRepository.existsById("crs_nope")).thenReturn(false);

        assertThatThrownBy(() -> authService.register(learner("ada", "crs_nope")))
                .isInstanceOf(NotFoundException.class);

        verifyNoInteractions(appUserRepository, cohortMemberRepository);
    }

    // ---------- trainer registration ----------

    @Test
    void registerTrainer_createsUserWithoutAnyCohortMembership() {
        stubSaveAssignsId("usr_T1");

        MeResponseDto result = authService.register(trainer("tom", null));

        ArgumentCaptor<AppUserModel> saved = ArgumentCaptor.forClass(AppUserModel.class);
        verify(appUserRepository).saveAndFlush(saved.capture());
        assertThat(saved.getValue().getRole()).isEqualTo(Role.trainer);
        verifyNoInteractions(cohortMemberRepository, cohortRepository);
        assertThat(result.role()).isEqualTo(Role.trainer);
        assertThat(result.cohortIds()).isEmpty();
    }

    @Test
    void registerTrainer_ignoresSuppliedCohortId() {
        stubSaveAssignsId("usr_T1");

        authService.register(trainer("tom", COHORT));

        verifyNoInteractions(cohortMemberRepository, cohortRepository);
    }

    // ---------- duplicate username ----------

    @Test
    void register_duplicateUsername_isConflictAndNothingIsSaved() {
        when(cohortRepository.existsById(COHORT)).thenReturn(true);
        when(appUserRepository.existsByUsername("ada")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(learner("ada", COHORT))).isInstanceOf(ConflictException.class);

        verify(appUserRepository, never()).saveAndFlush(any());
        verifyNoInteractions(cohortMemberRepository);
    }

    @Test
    void register_duplicateUsernameDetectedByDatabaseRace_isConflict() {
        // the exists-check passes, but another request inserts the same username first
        when(cohortRepository.existsById(COHORT)).thenReturn(true);
        when(appUserRepository.existsByUsername("ada")).thenReturn(false);
        when(appUserRepository.saveAndFlush(any(AppUserModel.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate key value violates unique constraint"));

        assertThatThrownBy(() -> authService.register(learner("ada", COHORT)))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Username");

        verifyNoInteractions(cohortMemberRepository);
    }

    @Test
    void register_duplicateTrainerUsernameRace_isConflict() {
        when(appUserRepository.saveAndFlush(any(AppUserModel.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate key"));

        assertThatThrownBy(() -> authService.register(trainer("tom", null))).isInstanceOf(ConflictException.class);
    }

    // ---------- login ----------

    @Test
    void login_withCorrectCredentials_returnsBearerToken() {
        AppUserModel user = AppUserModel.builder().userId("usr_L1").username("ada").role(Role.learner).build();
        when(appUserRepository.findByUsername("ada")).thenReturn(Optional.of(user));
        Instant now = Instant.now();
        when(jwtEncoder.encode(any(JwtEncoderParameters.class)))
                .thenReturn(new Jwt("the-token", now, now.plusSeconds(60), Map.of("alg", "HS256"), Map.of("sub", "usr_L1")));

        LoginResponseDto response = authService.login(new LoginRequestDto("  ada ", "Password123!"));

        assertThat(response.accessToken()).isEqualTo("the-token");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresIn()).isEqualTo(8 * 3600);

        // the token carries the usr_ id as subject and the role as claim
        ArgumentCaptor<JwtEncoderParameters> params = ArgumentCaptor.forClass(JwtEncoderParameters.class);
        verify(jwtEncoder).encode(params.capture());
        assertThat(params.getValue().getClaims().getSubject()).isEqualTo("usr_L1");
        assertThat((String) params.getValue().getClaims().getClaim("role")).isEqualTo("learner");
    }

    @Test
    void login_withBadCredentials_propagatesBadCredentialsAndIssuesNoToken() {
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("bad"));

        assertThatThrownBy(() -> authService.login(new LoginRequestDto("ada", "wrong-password")))
                .isInstanceOf(BadCredentialsException.class);

        verifyNoInteractions(jwtEncoder);
    }

    // ---------- GET /me ----------

    @Test
    void me_learner_returnsAssignedCohort() {
        AppUserModel user = AppUserModel.builder().userId("usr_L1").firstName("Ada").lastName("Lovelace")
                .role(Role.learner).build();
        when(appUserRepository.findById("usr_L1")).thenReturn(Optional.of(user));
        when(cohortMemberRepository.findByUserId("usr_L1")).thenReturn(List.of(new CohortMemberModel(COHORT, "usr_L1")));

        MeResponseDto me = authService.me("usr_L1");

        assertThat(me.role()).isEqualTo(Role.learner);
        assertThat(me.cohortIds()).containsExactly(COHORT);
    }

    @Test
    void me_trainer_hasEmptyCohortIds() {
        AppUserModel user = AppUserModel.builder().userId("usr_T1").firstName("Tom").lastName("Trainer")
                .role(Role.trainer).build();
        when(appUserRepository.findById("usr_T1")).thenReturn(Optional.of(user));

        assertThat(authService.me("usr_T1").cohortIds()).isEmpty();
        verifyNoInteractions(cohortMemberRepository);
    }

    @Test
    void me_displayNameIsCappedAtSpecLength() {
        AppUserModel user = AppUserModel.builder().userId("usr_T1").firstName("A".repeat(100)).lastName("B".repeat(100))
                .role(Role.trainer).build();
        when(appUserRepository.findById("usr_T1")).thenReturn(Optional.of(user));

        assertThat(authService.me("usr_T1").displayName()).hasSize(120);
    }

    @Test
    void me_userNoLongerExists_isBadCredentials() {
        when(appUserRepository.findById("usr_gone")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.me("usr_gone")).isInstanceOf(BadCredentialsException.class);
    }

    // ---------- GET /auth/cohorts ----------

    @Test
    void listCohorts_returnsIdsAndNames() {
        when(cohortRepository.findAll(any(Sort.class))).thenReturn(List.of(
                CohortModel.builder().cohortId("crs_A").name("Alpha").build(),
                CohortModel.builder().cohortId("crs_B").name("Beta").build()));

        assertThat(authService.listCohorts())
                .containsExactly(new CohortOptionDto("crs_A", "Alpha"), new CohortOptionDto("crs_B", "Beta"));
    }
}
