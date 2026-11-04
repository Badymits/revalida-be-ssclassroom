package org.ssglobal.revalida.ssclassroom.exceptions;

import org.ssglobal.revalida.ssclassroom.enums.ErrorCode;

public class ForbiddenException extends ApiException {

    private static final long serialVersionUID = 1L;

    public ForbiddenException(String message) {
        super(ErrorCode.FORBIDDEN, message);
    }
}
