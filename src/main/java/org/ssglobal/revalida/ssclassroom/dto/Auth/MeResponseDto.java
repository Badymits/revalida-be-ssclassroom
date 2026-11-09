package org.ssglobal.revalida.ssclassroom.dto.Auth;

import java.util.List;

import org.ssglobal.revalida.ssclassroom.enums.Role;
import org.ssglobal.revalida.ssclassroom.model.AppUserModel;

public record MeResponseDto(String id, Role role, String displayName, List<String> cohortIds) {

    public static MeResponseDto of(AppUserModel user, List<String> cohortIds) {
        // names are stored encrypted and never decrypted here, so the username (1-50 chars) is the display name
        return new MeResponseDto(
                user.getUserId(),
                user.getRole(),
                user.getUsername(),
                cohortIds);
    }
}
