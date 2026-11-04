package org.ssglobal.revalida.ssclassroom.error;

import java.util.List;

public record ApiError(Body error) {

    public record Body(String code, String message, List<Detail> details) {
    }

    public record Detail(String field, String message) {
    }

    public static ApiError of(ErrorCode code, String message) {
        return of(code, message, List.of());
    }

    public static ApiError of(ErrorCode code, String message, List<Detail> details) {
        return new ApiError(new Body(code.name(), message, details));
    }
}
