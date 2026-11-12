package org.ssglobal.revalida.ssclassroom.dto.Auth;

import java.util.List;

import org.ssglobal.revalida.ssclassroom.enums.Role;
import org.ssglobal.revalida.ssclassroom.model.AppUserModel;

public record MeResponseDto(String id, Role role, String displayName, List<String> cohortIds) {

    public static MeResponseDto of(AppUserModel user, List<String> cohortIds) {
        // API spec: displayName is 1-120 characters
        String displayName = (user.getFirstName() + " " + user.getLastName()).trim();
        if (displayName.length() > 120) {
            displayName = displayName.substring(0, 120);
        }
        return new MeResponseDto(
                user.getUserId(),
                user.getRole(),
                displayName,
                cohortIds);
    }
}
