package org.ssglobal.revalida.ssclassroom.dto.Auth;

public record LoginResponseDto(String accessToken, String tokenType, long expiresIn) {

    public static LoginResponseDto bearer(String accessToken, long expiresInSeconds) {
        return new LoginResponseDto(accessToken, "Bearer", expiresInSeconds);
    }
}
