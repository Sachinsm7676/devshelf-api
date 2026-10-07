package com.devshelf.api.book.dto;

import com.devshelf.api.book.BookCategory;
import com.devshelf.api.common.ValidationFailedException;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import static com.devshelf.api.book.dto.BookMessages.*;

/**
 * Validated parameters of {@code GET /books}.
 *
 * @param q        trimmed search text, empty for "no filter"
 * @param category the category to filter by, or {@code null} for all
 * @param page     1-based page number requested (may exceed the last page; the service clamps it)
 */
public record BookSearchQuery(String q, BookCategory category, BookSort sort, int page, int size) {

    public static final int MAX_Q_LENGTH = 100;
    public static final int DEFAULT_SIZE = 8;
    public static final int MAX_SIZE = 50;
    public static final String ALL_CATEGORIES = "All";

    /**
     * Parses raw query parameters. Absent or blank values take their defaults; every invalid
     * parameter is reported at once.
     *
     * @throws ValidationFailedException with one message per invalid parameter
     */
    public static BookSearchQuery parse(String q, String category, String sort, String page, String size) {
        Map<String, String> errors = new LinkedHashMap<>();

        String text = q == null ? "" : q.strip();
        if (text.length() > MAX_Q_LENGTH) {
            errors.put("q", Q_LENGTH);
        }

        BookCategory chosen = null;
        if (!isBlank(category) && !ALL_CATEGORIES.equalsIgnoreCase(category.strip())) {
            chosen = BookCategory.findByLabel(category).orElse(null);
            if (chosen == null) {
                errors.put("category", CATEGORY_UNKNOWN);
            }
        }

        BookSort order = BookSort.DEFAULT;
        if (!isBlank(sort)) {
            order = BookSort.fromParam(sort.strip()).orElse(null);
            if (order == null) {
                errors.put("sort", SORT_UNKNOWN);
            }
        }

        Integer pageNumber = isBlank(page) ? Integer.valueOf(1) : parseInt(page).filter(p -> p >= 1).orElse(null);
        if (pageNumber == null) {
            errors.put("page", PAGE_INVALID);
        }

        Integer pageSize = isBlank(size) ? Integer.valueOf(DEFAULT_SIZE)
                : parseInt(size).filter(s -> s >= 1 && s <= MAX_SIZE).orElse(null);
        if (pageSize == null) {
            errors.put("size", SIZE_INVALID);
        }

        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
        return new BookSearchQuery(text, chosen, order, pageNumber, pageSize);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static Optional<Integer> parseInt(String value) {
        try {
            return Optional.of(Integer.parseInt(value.strip()));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }
}
