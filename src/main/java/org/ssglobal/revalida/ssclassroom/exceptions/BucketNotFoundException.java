package org.ssglobal.revalida.ssclassroom.exceptions;

public class BucketNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public BucketNotFoundException(String message) {
        super(message);
    }
}
