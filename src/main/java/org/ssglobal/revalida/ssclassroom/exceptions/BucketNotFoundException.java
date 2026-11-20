package org.ssglobal.revalida.ssclassroom.exceptions;

import java.io.Serial;

public class BucketNotFoundException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public BucketNotFoundException(String message) {
        super(message);
    }
}
