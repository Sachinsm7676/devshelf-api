package com.devshelf.api.book;

import com.devshelf.api.book.dto.BookRequest;
import com.devshelf.api.common.ValidationFailedException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

import static com.devshelf.api.book.dto.BookMessages.*;

/**
 * Validates a {@link BookRequest}: Bean Validation for the declarative rules, then the ordered rules
 * that need more than one check per field. At most one message is reported per field, the first rule
 * that fails, and fields are reported in form order.
 */
@Component
public class BookRequestValidator {

    static final BigDecimal MAX_PRICE = new BigDecimal("99999.99");

    private static final Pattern DATE_SHAPE = Pattern.compile("\\d{4}-\\d{2}-\\d{2}");

    private final Validator validator;
    private final Clock clock;

    public BookRequestValidator(Validator validator, Clock clock) {
        this.validator = validator;
        this.clock = clock;
    }

    /** @throws ValidationFailedException if any field is invalid */
    public void validate(BookRequest request) {
        Map<String, String> errors = new HashMap<>();
        for (ConstraintViolation<BookRequest> violation : validator.validate(request)) {
            errors.putIfAbsent(violation.getPropertyPath().toString(), violation.getMessage());
        }

        if (!errors.containsKey("priceInr") && request.priceInr() != null) {
            BigDecimal price = request.priceInr();
            if (price.signum() < 0 || price.compareTo(MAX_PRICE) > 0) {
                errors.put("priceInr", PRICE_RANGE);
            } else if (price.stripTrailingZeros().scale() > 2) {
                errors.put("priceInr", PRICE_DECIMALS);
            }
        }

        if (request.publishedAt() != null) {
            LocalDate date = parseDate(request.publishedAt());
            if (date == null) {
                errors.put("publishedAt", PUBLISHED_AT_FORMAT);
            } else if (date.isAfter(LocalDate.now(clock))) {
                errors.put("publishedAt", PUBLISHED_AT_FUTURE);
            }
        }

        if (!errors.containsKey("coverUrl") && request.coverUrl() != null && !hasAllowedPrefix(request.coverUrl())) {
            errors.put("coverUrl", COVER_URL_PREFIX);
        }

        if (!errors.isEmpty()) {
            throw new ValidationFailedException(inFormOrder(errors));
        }
    }

    /** Parses a strict {@code YYYY-MM-DD} date, or returns {@code null} if the text is not one. */
    static LocalDate parseDate(String text) {
        if (text == null || !DATE_SHAPE.matcher(text).matches()) {
            return null;
        }
        try {
            return LocalDate.parse(text, DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private static boolean hasAllowedPrefix(String url) {
        return url.startsWith("http://") || url.startsWith("https://") || url.startsWith("/assets/images/");
    }

    private static Map<String, String> inFormOrder(Map<String, String> errors) {
        Map<String, String> ordered = new LinkedHashMap<>();
        FIELD_ORDER.forEach(field -> {
            if (errors.containsKey(field)) {
                ordered.put(field, errors.get(field));
            }
        });
        errors.forEach(ordered::putIfAbsent);
        return ordered;
    }
}
