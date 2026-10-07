package com.devshelf.api.common;

import java.util.Map;

/** The request is valid but clashes with existing data. Rendered as 409. */
public class ConflictException extends RuntimeException {

    private final String code;
    private final Map<String, String> fieldErrors;

    public ConflictException(String code, String message, Map<String, String> fieldErrors) {
        super(message);
        this.code = code;
        this.fieldErrors = Map.copyOf(fieldErrors);
    }

    public String getCode() {
        return code;
    }

    public Map<String, String> getFieldErrors() {
        return fieldErrors;
    }
}
