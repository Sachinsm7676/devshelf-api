package com.devshelf.api.book.dto;

import com.devshelf.api.book.BookCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

import static com.devshelf.api.book.dto.BookMessages.*;

/**
 * Body of POST and PUT: the editable fields only. Strings are trimmed on arrival and an optional
 * field that is blank becomes {@code null}.
 * <p>
 * The annotations cover the single-rule checks and never overlap, so each field yields at most one
 * message. Ordered rules (price range then decimals, date format then "not in the future", cover URL
 * length then prefix) live in {@code BookRequestValidator}.
 */
public record BookRequest(

        @Schema(example = "Clean Code in Java", minLength = 2, maxLength = 120)
        @NotBlank(message = TITLE_REQUIRED)
        @Size(min = 2, max = 120, message = TITLE_LENGTH)
        String title,

        @Schema(example = "Ananya Rao", minLength = 2, maxLength = 80)
        @NotBlank(message = AUTHOR_REQUIRED)
        @Size(min = 2, max = 80, message = AUTHOR_LENGTH)
        String author,

        @Schema(example = "Java", allowableValues = {
                "JavaScript", "Java", "Python", "DevOps", "System Design", "AI/ML", "Databases"})
        @NotNull(message = CATEGORY_REQUIRED)
        BookCategory category,

        @Schema(example = "999.00", minimum = "0", maximum = "99999.99")
        @NotNull(message = PRICE_REQUIRED)
        BigDecimal priceInr,

        @Schema(example = "9789350000014", nullable = true, pattern = "^\\d{13}$")
        @Pattern(regexp = "\\d{13}", message = ISBN_FORMAT)
        String isbn,

        @Schema(type = "string", format = "date", example = "2025-11-04", nullable = true)
        String publishedAt,

        @Schema(nullable = true, maxLength = 1000)
        @Size(max = 1000, message = DESCRIPTION_LENGTH)
        String description,

        @Schema(example = "/assets/images/book-cover-clean-code-in-java.jpg", nullable = true, maxLength = 500)
        @Size(max = 500, message = COVER_URL_LENGTH)
        String coverUrl) {

    public BookRequest {
        title = trimToNull(title);
        author = trimToNull(author);
        isbn = trimToNull(isbn);
        publishedAt = trimToNull(publishedAt);
        description = trimToNull(description);
        coverUrl = trimToNull(coverUrl);
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.strip();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
