package org.ssglobal.revalida.ssclassroom.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record DraftRequestDto(
        @Min(0) Integer points,
        @Size(max = 5000) String privateComment) {
}