package org.ssglobal.revalida.ssclassroom.error.test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.ssglobal.revalida.ssclassroom.error.ApiError;
import org.ssglobal.revalida.ssclassroom.error.ApiErrorController;
import org.ssglobal.revalida.ssclassroom.error.ConflictException;
import org.ssglobal.revalida.ssclassroom.error.ErrorCode;
import org.ssglobal.revalida.ssclassroom.error.GlobalExceptionHandler;
import org.ssglobal.revalida.ssclassroom.error.NotFoundException;
import org.ssglobal.revalida.ssclassroom.error.StateInvalidException;

import jakarta.servlet.RequestDispatcher;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;

class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new TestController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void notFoundExceptionReturns404() throws Exception {
        mockMvc.perform(get("/test/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.error.message").value("Submission sub_123 not found"))
                .andExpect(jsonPath("$.error.details").isEmpty());
    }

    @Test
    void stateInvalidExceptionReturns422() throws Exception {
        mockMvc.perform(get("/test/state-invalid"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.error.code").value("STATE_INVALID"))
                .andExpect(jsonPath("$.error.message").value("Only submitted work can be returned"));
    }

    @Test
    void conflictExceptionReturns409() throws Exception {
        mockMvc.perform(get("/test/conflict"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("CONFLICT"));
    }

    @Test
    void invalidBodyReturns400WithFieldDetails() throws Exception {
        mockMvc.perform(post("/test/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\": \"\", \"points\": 150}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.details.length()").value(2))
                .andExpect(jsonPath("$.error.details[?(@.field == 'title')]").exists())
                .andExpect(jsonPath("$.error.details[?(@.field == 'points')]").exists());
    }

    @Test
    void brokenJsonReturns400() throws Exception {
        mockMvc.perform(post("/test/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ this is not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void unexpectedErrorReturns500WithoutLeakingDetails() throws Exception {
        mockMvc.perform(get("/test/crash"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.error.message").value("Something went wrong"))
                .andExpect(content().string(not(containsString("secret"))));
    }

    @Test
    void errorControllerUsesEnvelope() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(RequestDispatcher.ERROR_STATUS_CODE, 404);

        ResponseEntity<ApiError> response = new ApiErrorController().handleError(request);

        assertThat(response.getStatusCode().value()).isEqualTo(404);
        assertThat(response.getBody().error().code()).isEqualTo("NOT_FOUND");
    }

    @Test
    void fromStatusFindsTheRightCode() {
        assertThat(ErrorCode.fromStatus(404)).isEqualTo(ErrorCode.NOT_FOUND);
        assertThat(ErrorCode.fromStatus(422)).isEqualTo(ErrorCode.STATE_INVALID);
        assertThat(ErrorCode.fromStatus(405)).isEqualTo(ErrorCode.VALIDATION_ERROR);
        assertThat(ErrorCode.fromStatus(503)).isEqualTo(ErrorCode.INTERNAL_ERROR);
    }

    record TestRequest(@NotBlank String title, @Max(100) Integer points) {
    }

    @RestController
    static class TestController {

        @GetMapping("/test/not-found")
        void notFound() {
            throw NotFoundException.of("Submission", "sub_123");
        }

        @GetMapping("/test/state-invalid")
        void stateInvalid() {
            throw new StateInvalidException("Only submitted work can be returned");
        }

        @GetMapping("/test/conflict")
        void conflict() {
            throw new ConflictException("Assignment is already published");
        }

        @PostMapping("/test/validate")
        void validate(@Valid @RequestBody TestRequest request) {
        }

        @GetMapping("/test/crash")
        void crash() {
            throw new IllegalStateException("secret database password is wrong");
        }
    }
}
