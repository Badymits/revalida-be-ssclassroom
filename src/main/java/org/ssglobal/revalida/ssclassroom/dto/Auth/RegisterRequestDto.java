package org.ssglobal.revalida.ssclassroom.dto.Auth;

import org.ssglobal.revalida.ssclassroom.enums.Role;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Self-registration.
 * role "learner": cohortId is required and the learner gets exactly one cohort_member row.
 * role "trainer": cohortId is ignored; trainers have global access and never get a cohort_member row.
 */
public record RegisterRequestDto(
        @NotBlank @Size(max = 600) String firstName,
        @NotBlank @Size(max = 600) String lastName,
        @NotBlank @Size(max = 50) String username,
        // 72 = BCrypt's byte limit
        @NotBlank @Size(min = 8, max = 72) String password,
        @NotNull Role role,
        @Size(max = 30) String cohortId) {
}
