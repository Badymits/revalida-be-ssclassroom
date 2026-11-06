package org.ssglobal.revalida.ssclassroom.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReturnRequestDto(
        @NotNull @Min(0) Integer points,
        @Size(max = 5000) String privateComment) {
}