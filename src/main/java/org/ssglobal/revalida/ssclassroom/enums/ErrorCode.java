package org.ssglobal.revalida.ssclassroom.enums;

import java.util.Arrays;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    VALIDATION_ERROR(400, "The request is not valid"),
    UNAUTHENTICATED(401, "Authentication is required"),
    FORBIDDEN(403, "You do not have permission to do this"),
    NOT_FOUND(404, "The requested resource was not found"),
    CONFLICT(409, "The request conflicts with existing data"),
    PAYLOAD_TOO_LARGE(413, "The file is too large"),
    STATE_INVALID(422, "This action is not allowed in the current state"),
    INTERNAL_ERROR(500, "Something went wrong");

    private final int status;
    private final String defaultMessage;

    // Statuses without their own code (e.g. 405, 415) fall back to VALIDATION_ERROR or INTERNAL_ERROR.
    public static ErrorCode fromStatus(int status) {
        return Arrays.stream(values())
                .filter(code -> code.status == status)
                .findFirst()
                .orElse(status >= 400 && status < 500 ? VALIDATION_ERROR : INTERNAL_ERROR);
    }
}
