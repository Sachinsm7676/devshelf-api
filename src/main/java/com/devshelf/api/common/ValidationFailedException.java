package com.devshelf.api.common;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** One or more request fields are invalid. Rendered as 400 {@code VALIDATION_FAILED}. */
public class ValidationFailedException extends RuntimeException {

    private final Map<String, String> fieldErrors;

    /** @param fieldErrors field name to message; iteration order is preserved in the response */
    public ValidationFailedException(Map<String, String> fieldErrors) {
        super("Validation failed: " + fieldErrors.keySet());
        this.fieldErrors = Collections.unmodifiableMap(new LinkedHashMap<>(fieldErrors));
    }

    public Map<String, String> getFieldErrors() {
        return fieldErrors;
    }
}
