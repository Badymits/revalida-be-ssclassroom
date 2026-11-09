package org.ssglobal.revalida.ssclassroom.auth.api.test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.ssglobal.revalida.ssclassroom.config.MySecurityContext;
import org.ssglobal.revalida.ssclassroom.controller.AuthController;
import org.ssglobal.revalida.ssclassroom.controller.ContentController;
import org.ssglobal.revalida.ssclassroom.controller.ReviewController;
import org.ssglobal.revalida.ssclassroom.controller.SubmissionController;
import org.ssglobal.revalida.ssclassroom.dto.Auth.CohortOptionDto;
import org.ssglobal.revalida.ssclassroom.dto.Auth.LoginResponseDto;
import org.ssglobal.revalida.ssclassroom.dto.Auth.MeResponseDto;
import org.ssglobal.revalida.ssclassroom.enums.Role;
import org.ssglobal.revalida.ssclassroom.error.ApiErrorWriter;
import org.ssglobal.revalida.ssclassroom.error.RestAccessDeniedHandler;
import org.ssglobal.revalida.ssclassroom.error.RestAuthenticationEntryPoint;
import org.ssglobal.revalida.ssclassroom.exceptions.BadRequestException;
import org.ssglobal.revalida.ssclassroom.exceptions.ConflictException;
import org.ssglobal.revalida.ssclassroom.exceptions.NotFoundException;
import org.ssglobal.revalida.ssclassroom.repository.AppUserRepository;
import org.ssglobal.revalida.ssclassroom.service.AssignmentService;
import org.ssglobal.revalida.ssclassroom.service.AuthService;
import org.ssglobal.revalida.ssclassroom.service.GradeService;
import org.ssglobal.revalida.ssclassroom.service.ReviewService;
import org.ssglobal.revalida.ssclassroom.service.SubmissionService;

/**
 * Loads the REAL MySecurityContext (JWT decoder, role mapping, URL rules, error handlers) with the controllers.
 * Only the services are mocked, so no database is needed.
 */
@WebMvcTest(controllers = { AuthController.class, ContentController.class,
        ReviewController.class, SubmissionController.class })
@Import({ MySecurityContext.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class,
        ApiErrorWriter.class })
@TestPropertySource(properties = {
        "app.jwt.secret=test-secret-test-secret-test-secret-0123456789",
        "app.jwt.expiration-hours=1" })
class SecurityApiTest {

    @Autowired MockMvc mvc;
    @Autowired JwtEncoder jwtEncoder;

    @MockitoBean AuthService authService;
    @MockitoBean AppUserRepository appUserRepository; // used by the UserDetailsService bean
    @MockitoBean AssignmentService assignmentService;
    @MockitoBean GradeService gradeService;
    @MockitoBean ReviewService reviewService;
    @MockitoBean SubmissionService submissionService;

    // ---------- helpers ----------

    // same shape as AuthService's token: subject = usr_ id, "role" claim = learner | trainer
    private String bearer(String userId, Role role) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder().subject(userId).claim("role", role.name())
                .issuedAt(now).expiresAt(now.plusSeconds(600)).build();
        return "Bearer " + jwtEncoder.encode(
                JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
    }

    private String learnerToken() { return bearer("usr_L1", Role.learner); }
    private String trainerToken() { return bearer("usr_T1", Role.trainer); }

    private static final String REGISTER_LEARNER = """
            {"firstName":"Ada","lastName":"Lovelace","username":"ada","password":"Password123!",
             "role":"learner","cohortId":"crs_SEED_BACKEND_Q4"}""";
    private static final String REGISTER_TRAINER = """
            {"firstName":"Tom","lastName":"Trainer","username":"tom","password":"Password123!","role":"trainer"}""";

    // ---------- 401 Unauthorized ----------

    @Test
    void me_withoutToken_returns401WithErrorEnvelope() throws Exception {
        mvc.perform(get("/api/v1/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHENTICATED"));
    }

    @Test
    void me_withGarbageToken_returns401() throws Exception {
        mvc.perform(get("/api/v1/me").header("Authorization", "Bearer not.a.jwt"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHENTICATED"));
    }

    @Test
    void me_withExpiredToken_returns401() throws Exception {
        Instant longAgo = Instant.now().minusSeconds(3600);
        JwtClaimsSet claims = JwtClaimsSet.builder().subject("usr_L1").claim("role", "learner")
                .issuedAt(longAgo.minusSeconds(60)).expiresAt(longAgo).build();
        String expired = jwtEncoder.encode(
                JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();

        mvc.perform(get("/api/v1/me").header("Authorization", "Bearer " + expired))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void me_withTokenSignedByAnotherSecret_returns401() throws Exception {
        var otherKey = new SecretKeySpec(
                "another-secret-another-secret-9876543210".getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        JwtEncoder foreign = new NimbusJwtEncoder(new com.nimbusds.jose.jwk.source.ImmutableSecret<>(otherKey));
        JwtClaimsSet claims = JwtClaimsSet.builder().subject("usr_T1").claim("role", "trainer")
                .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(600)).build();
        String forged = foreign.encode(
                JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();

        mvc.perform(get("/api/v1/me").header("Authorization", "Bearer " + forged))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedRoute_withoutToken_returns401() throws Exception {
        mvc.perform(get("/api/v1/assignments/a-1")).andExpect(status().isUnauthorized());
    }

    // ---------- GET /me ----------

    @Test
    void me_asLearner_returnsProfileOfTokenSubject() throws Exception {
        when(authService.me("usr_L1"))
                .thenReturn(new MeResponseDto("usr_L1", Role.learner, "Ada Lovelace", List.of("crs_1")));

        mvc.perform(get("/api/v1/me").header("Authorization", learnerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("usr_L1"))
                .andExpect(jsonPath("$.role").value("learner"))
                .andExpect(jsonPath("$.displayName").value("Ada Lovelace"))
                .andExpect(jsonPath("$.cohortIds[0]").value("crs_1"));

        verify(authService).me("usr_L1");
    }

    @Test
    void me_asTrainer_hasNoCohorts() throws Exception {
        when(authService.me("usr_T1"))
                .thenReturn(new MeResponseDto("usr_T1", Role.trainer, "Tom Trainer", List.of()));

        mvc.perform(get("/api/v1/me").header("Authorization", trainerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("trainer"))
                .andExpect(jsonPath("$.cohortIds").isEmpty());
    }

    // ---------- role authority mapping ----------

    @Test
    void roleClaim_mapsToRoleAuthority_soEachRoleOnlyReachesItsOwnRoutes() throws Exception {
        // "trainer" claim -> ROLE_trainer: reaches the trainer route, not the learner route
        mvc.perform(patch("/api/v1/assignments/a-1").header("Authorization", trainerToken())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"x\"}"))
                .andExpect(status().isOk());
        mvc.perform(post("/api/v1/submissions/s-1/unsubmit").header("Authorization", trainerToken()))
                .andExpect(status().isForbidden());

        // "learner" claim -> ROLE_learner: the other way round
        mvc.perform(patch("/api/v1/assignments/a-1").header("Authorization", learnerToken())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"x\"}"))
                .andExpect(status().isForbidden());
        MvcResult learnerRoute = mvc.perform(post("/api/v1/submissions/s-1/unsubmit")
                        .header("Authorization", learnerToken())).andReturn();
        assertThat(learnerRoute.getResponse().getStatus()).isNotIn(401, 403);
    }

    // ---------- 403 Forbidden / role rules ----------

    @Test
    void trainerOnlyRoutes_forLearner_return403WithErrorEnvelope() throws Exception {
        String learner = learnerToken();

        mvc.perform(patch("/api/v1/assignments/a-1").header("Authorization", learner)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"x\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));

        mvc.perform(post("/api/v1/assignments/a-1/publish").header("Authorization", learner)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"cohortId\":\"crs_1\"}"))
                .andExpect(status().isForbidden());

        mvc.perform(post("/api/v1/cohorts/crs_1/assignments").header("Authorization", learner)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());

        mvc.perform(get("/api/v1/assignments/a-1/submissions").header("Authorization", learner))
                .andExpect(status().isForbidden());

        mvc.perform(post("/api/v1/submissions/s-1/return").header("Authorization", learner)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"points\":10}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void trainerOnlyRoute_forTrainer_isAllowedThrough() throws Exception {
        mvc.perform(patch("/api/v1/assignments/a-1").header("Authorization", trainerToken())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"x\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void learnerOnlyRoutes_forTrainer_return403() throws Exception {
        String trainer = trainerToken();

        mvc.perform(get("/api/v1/assignments/a-1/my-submission").header("Authorization", trainer)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());

        mvc.perform(post("/api/v1/submissions/s-1/turn-in").header("Authorization", trainer)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());

        mvc.perform(post("/api/v1/submissions/s-1/unsubmit").header("Authorization", trainer))
                .andExpect(status().isForbidden());

        mvc.perform(post("/api/v1/submissions/s-1/upload-url").header("Authorization", trainer)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void learnerOnlyRoute_forLearner_isNotBlockedBySecurity() throws Exception {
        MvcResult result = mvc.perform(post("/api/v1/submissions/s-1/unsubmit")
                        .header("Authorization", learnerToken()))
                .andReturn();

        assertThat(result.getResponse().getStatus()).isNotIn(401, 403);
    }

    @Test
    void sharedRoute_isAvailableToBothRoles() throws Exception {
        mvc.perform(get("/api/v1/assignments/a-1").header("Authorization", learnerToken()))
                .andExpect(status().isOk());
        mvc.perform(get("/api/v1/assignments/a-1").header("Authorization", trainerToken()))
                .andExpect(status().isOk());
    }

    @Test
    void csrfIsNotRequired_forTokenAuthenticatedPosts() throws Exception {
        // a POST without a CSRF token must not be rejected with 403 for a legitimate trainer call
        mvc.perform(post("/api/v1/assignments/a-1/publish").header("Authorization", trainerToken())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"cohortId\":\"crs_1\"}"))
                .andExpect(status().isOk());
    }

    // ---------- public endpoints ----------

    @Test
    void login_isPublic() throws Exception {
        when(authService.login(any())).thenReturn(LoginResponseDto.bearer("jwt-value", 3600));

        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"ada\",\"password\":\"Password123!\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("jwt-value"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"));
    }

    @Test
    void login_withBlankFields_returns400() throws Exception {
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"\",\"password\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void cohorts_isPublic_andReturnsIdsAndNames() throws Exception {
        when(authService.listCohorts()).thenReturn(List.of(
                new CohortOptionDto("crs_SEED_BACKEND_Q4", "CDP Backend Q4"),
                new CohortOptionDto("crs_SEED_FRONTEND_Q4", "CDP Frontend Q4")));

        mvc.perform(get("/api/v1/auth/cohorts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value("crs_SEED_BACKEND_Q4"))
                .andExpect(jsonPath("$[0].name").value("CDP Backend Q4"));
    }

    @Test
    void checkConnection_isPublic() throws Exception {
        mvc.perform(get("/api/v1/check-connection")).andExpect(status().isOk());
    }

    // ---------- registration over HTTP ----------

    @Test
    void registerLearner_returns201() throws Exception {
        when(authService.register(any())).thenReturn(
                new MeResponseDto("usr_L9", Role.learner, "Ada Lovelace", List.of("crs_SEED_BACKEND_Q4")));

        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(REGISTER_LEARNER))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("learner"))
                .andExpect(jsonPath("$.cohortIds[0]").value("crs_SEED_BACKEND_Q4"));
    }

    @Test
    void registerTrainer_returns201WithNoCohorts() throws Exception {
        when(authService.register(any())).thenReturn(
                new MeResponseDto("usr_T9", Role.trainer, "Tom Trainer", List.of()));

        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(REGISTER_TRAINER))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("trainer"))
                .andExpect(jsonPath("$.cohortIds").isEmpty());
    }

    @Test
    void register_duplicateUsername_returns409() throws Exception {
        when(authService.register(any())).thenThrow(new ConflictException("Username is already taken"));

        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(REGISTER_LEARNER))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("CONFLICT"));
    }

    @Test
    void register_learnerWithoutCohort_returns400() throws Exception {
        when(authService.register(any()))
                .thenThrow(new BadRequestException("cohortId", "cohortId is required for learners"));

        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\"Ada\",\"lastName\":\"L\",\"username\":\"ada\","
                                + "\"password\":\"Password123!\",\"role\":\"learner\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void register_unknownCohort_returns404() throws Exception {
        when(authService.register(any())).thenThrow(NotFoundException.of("Cohort", "crs_nope"));

        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(REGISTER_LEARNER))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
    }

    @Test
    void register_missingRole_returns400() throws Exception {
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\"Ada\",\"lastName\":\"L\",\"username\":\"ada\","
                                + "\"password\":\"Password123!\",\"cohortId\":\"crs_1\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void register_unsupportedRole_returns400() throws Exception {
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\"Ada\",\"lastName\":\"L\",\"username\":\"ada\","
                                + "\"password\":\"Password123!\",\"role\":\"admin\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void register_shortPassword_returns400() throws Exception {
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\"Ada\",\"lastName\":\"L\",\"username\":\"ada\","
                                + "\"password\":\"short\",\"role\":\"trainer\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }
}
