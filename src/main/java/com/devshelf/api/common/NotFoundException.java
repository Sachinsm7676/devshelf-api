package com.devshelf.api.common;

/** The resource named in the URL does not exist. Rendered as 404 with the given code and message. */
public class NotFoundException extends RuntimeException {

    private final String code;

    public NotFoundException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
