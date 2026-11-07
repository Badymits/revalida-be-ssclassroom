package org.ssglobal.revalida.ssclassroom.dto.Auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequestDto(
        @NotBlank @Size(max = 50) String username,
        @NotBlank @Size(max = 72) String password) {
}
