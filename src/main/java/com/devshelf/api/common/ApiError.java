package com.devshelf.api.common;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

/**
 * The body of every non-2xx response.
 *
 * @param code        stable machine-readable code, e.g. {@code VALIDATION_FAILED}
 * @param message     plain-English text that can be shown to the user
 * @param fieldErrors one message per invalid field (or query parameter); empty when not field-specific
 */
public record ApiError(int status, String code, String message, Map<String, String> fieldErrors) {

    public static final String VALIDATION_MESSAGE = "Please correct the highlighted fields.";

    public static ResponseEntity<ApiError> of(HttpStatus status, String code, String message) {
        return of(status, code, message, Map.of());
    }

    public static ResponseEntity<ApiError> of(HttpStatus status, String code, String message,
                                              Map<String, String> fieldErrors) {
        return ResponseEntity.status(status).body(new ApiError(status.value(), code, message, fieldErrors));
    }

    public static ResponseEntity<ApiError> validationFailed(Map<String, String> fieldErrors) {
        return of(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", VALIDATION_MESSAGE, fieldErrors);
    }
}
