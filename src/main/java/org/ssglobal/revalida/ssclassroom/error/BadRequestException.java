package org.ssglobal.revalida.ssclassroom.error;

import java.util.List;

public class BadRequestException extends ApiException {

    private static final long serialVersionUID = 1L;

    public BadRequestException(String message) {
        super(ErrorCode.VALIDATION_ERROR, message);
    }

    public BadRequestException(String field, String message) {
        super(ErrorCode.VALIDATION_ERROR, message, List.of(new ApiError.Detail(field, message)));
    }
}
