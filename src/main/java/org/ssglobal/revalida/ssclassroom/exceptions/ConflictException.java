package org.ssglobal.revalida.ssclassroom.exceptions;

import org.ssglobal.revalida.ssclassroom.enums.ErrorCode;

public class ConflictException extends ApiException {

    private static final long serialVersionUID = 1L;

    public ConflictException(String message) {
        super(ErrorCode.CONFLICT, message);
    }
}
