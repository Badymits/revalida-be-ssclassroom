package org.ssglobal.revalida.ssclassroom.exceptions;

import org.ssglobal.revalida.ssclassroom.enums.ErrorCode;

public class PayloadTooLargeException extends ApiException {

    private static final long serialVersionUID = 1L;

    public PayloadTooLargeException(String message) {
        super(ErrorCode.PAYLOAD_TOO_LARGE, message);
    }
}
