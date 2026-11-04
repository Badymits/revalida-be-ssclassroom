package org.ssglobal.revalida.ssclassroom.error;

public class NotFoundException extends ApiException {

    private static final long serialVersionUID = 1L;

    public NotFoundException(String message) {
        super(ErrorCode.NOT_FOUND, message);
    }

    public static NotFoundException of(String resource, String id) {
        return new NotFoundException(resource + " " + id + " not found");
    }
}
