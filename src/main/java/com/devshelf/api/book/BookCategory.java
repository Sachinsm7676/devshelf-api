package com.devshelf.api.book;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

/**
 * Book categories. The database stores the enum name; JSON and query parameters use the label.
 */
public enum BookCategory {

    JAVASCRIPT("JavaScript"),
    JAVA("Java"),
    PYTHON("Python"),
    DEVOPS("DevOps"),
    SYSTEM_DESIGN("System Design"),
    AI_ML("AI/ML"),
    DATABASES("Databases");

    private final String label;

    BookCategory(String label) {
        this.label = label;
    }

    @JsonValue
    public String label() {
        return label;
    }

    /** Finds a category by its label, ignoring case and surrounding whitespace. */
    public static Optional<BookCategory> findByLabel(String label) {
        if (label == null) {
            return Optional.empty();
        }
        String wanted = label.trim().toLowerCase(Locale.ROOT);
        return Arrays.stream(values())
                .filter(c -> c.label.toLowerCase(Locale.ROOT).equals(wanted))
                .findFirst();
    }

    /**
     * JSON entry point: a blank value means "not chosen" (null); an unknown label is rejected,
     * which the API reports as a field error on {@code category}.
     */
    @JsonCreator
    public static BookCategory fromJson(String label) {
        if (label == null || label.isBlank()) {
            return null;
        }
        return findByLabel(label)
                .orElseThrow(() -> new IllegalArgumentException("Unknown category: " + label));
    }
}
