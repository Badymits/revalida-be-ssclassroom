package org.ssglobal.revalida.ssclassroom.exceptions;

import org.ssglobal.revalida.ssclassroom.enums.ErrorCode;

public class StateInvalidException extends ApiException {

    private static final long serialVersionUID = 1L;

    public StateInvalidException(String message) {
        super(ErrorCode.STATE_INVALID, message);
    }
}
