package com.devshelf.api.book;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Map;
import java.util.StringJoiner;
import java.util.stream.Stream;

import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class BookValidationApiTest extends ApiTestSupport {

    private static final String ABSENT = "<absent>";

    @Test
    void emptyBodyReportsEveryRequiredField() throws Exception {
        postBook("{}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.message").value("Please correct the highlighted fields."))
                .andExpect(fieldErrorsAre(Map.of(
                        "title", "Title is required.",
                        "author", "Author is required.",
                        "category", "Choose a category.",
                        "priceInr", "Price is required.")));
    }

    /**
     * Each case replaces one field of a valid body with a raw JSON value and expects exactly one
     * error, on that field, with the first rule that fails.
     */
    @ParameterizedTest(name = "{0} = {1} -> {2}")
    @MethodSource("invalidFields")
    void reportsOneMessageForTheInvalidField(String field, String rawJson, String message) throws Exception {
        postBook(bodyWith(field, rawJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(fieldErrorsAre(Map.of(field, message)));
    }

    static Stream<Arguments> invalidFields() {
        return Stream.of(
                arguments("title", ABSENT, "Title is required."),
                arguments("title", "\"   \"", "Title is required."),
                arguments("title", "\"A\"", "Title must be 2 to 120 characters."),
                arguments("title", quoted("x".repeat(121)), "Title must be 2 to 120 characters."),
                arguments("author", "\"\"", "Author is required."),
                arguments("author", "\" B \"", "Author must be 2 to 80 characters."),
                arguments("author", quoted("y".repeat(81)), "Author must be 2 to 80 characters."),
                arguments("category", "null", "Choose a category."),
                arguments("category", "\"\"", "Choose a category."),
                arguments("category", "\"Rust\"", "Choose a category from the list."),
                arguments("category", "5", "Choose a category from the list."),
                arguments("priceInr", "null", "Price is required."),
                arguments("priceInr", "\"abc\"", "Price must be a number."),
                arguments("priceInr", "true", "Price must be a number."),
                arguments("priceInr", "-1", "Price must be between ₹0 and ₹99,999.99."),
                arguments("priceInr", "100000", "Price must be between ₹0 and ₹99,999.99."),
                arguments("priceInr", "99999.999", "Price must be between ₹0 and ₹99,999.99."),
                arguments("priceInr", "10.555", "Price can have at most 2 decimal places."),
                arguments("isbn", "\"12345\"", "ISBN must be exactly 13 digits."),
                arguments("isbn", "\"978935000001X\"", "ISBN must be exactly 13 digits."),
                arguments("isbn", "\"97893500000140\"", "ISBN must be exactly 13 digits."),
                arguments("publishedAt", "\"07-10-2026\"", "Enter the date as YYYY-MM-DD."),
                arguments("publishedAt", "\"2026-02-30\"", "Enter the date as YYYY-MM-DD."),
                arguments("publishedAt", "20261007", "Enter the date as YYYY-MM-DD."),
                arguments("publishedAt", "\"2026-10-09\"", "Published date cannot be in the future."),
                arguments("description", quoted("d".repeat(1001)), "Description can be at most 1,000 characters."),
                arguments("coverUrl", "\"ftp://example.com/cover.jpg\"",
                        "Cover URL must start with http://, https:// or /assets/images/."),
                arguments("coverUrl", "\"assets/images/cover.jpg\"",
                        "Cover URL must start with http://, https:// or /assets/images/."),
                arguments("coverUrl", quoted("https://example.com/" + "a".repeat(481)),
                        "Cover URL can be at most 500 characters."),
                arguments("coverUrl", quoted("ftp://" + "a".repeat(600)),
                        "Cover URL can be at most 500 characters."));
    }

    @Test
    void acceptsTheBoundaryValues() throws Exception {
        Map<String, Object> body = validBook("priceInr", 99999.99);
        body.put("title", "Go");
        body.put("author", "Al");
        body.put("description", "d".repeat(1000));
        body.put("coverUrl", "/assets/images/book-cover-clean-code-in-java.jpg");
        // 2026-10-08 is still tomorrow in UTC but already today in Asia/Kolkata (app.timezone).
        body.put("publishedAt", "2026-10-08");
        postBook(body).andExpect(status().isCreated());

        Map<String, Object> free = validBook("priceInr", 0);
        free.put("isbn", null);
        postBook(free).andExpect(status().isCreated());
    }

    @Test
    void unreadableJsonIsAGenericBadRequest() throws Exception {
        postBook("{\"title\": \"Broken")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message")
                        .value("The request could not be read. Please check the values and try again."))
                .andExpect(jsonPath("$.fieldErrors").isEmpty());
    }

    @Test
    void updateIsValidatedTheSameWay() throws Exception {
        putBook("clean-code-in-java", validBook("priceInr", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(fieldErrorsAre(Map.of("priceInr", "Price must be a number.")));
        putBook("clean-code-in-java", validBook("title", " "))
                .andExpect(status().isBadRequest())
                .andExpect(fieldErrorsAre(Map.of("title", "Title is required.")));
    }

    /** A valid body as raw JSON with {@code field} replaced by {@code rawJson} (or left out). */
    private static String bodyWith(String field, String rawJson) {
        Map<String, String> fields = new java.util.LinkedHashMap<>();
        fields.put("title", "\"Rust for Java Developers\"");
        fields.put("author", "\"Asha Verma\"");
        fields.put("category", "\"Java\"");
        fields.put("priceInr", "1099.50");
        fields.put("isbn", "\"9789350000991\"");
        fields.put("publishedAt", "\"2026-01-15\"");
        fields.put("description", "\"A short description.\"");
        fields.put("coverUrl", "\"https://example.com/rust.jpg\"");
        if (ABSENT.equals(rawJson)) {
            fields.remove(field);
        } else {
            fields.put(field, rawJson);
        }
        StringJoiner json = new StringJoiner(",", "{", "}");
        fields.forEach((key, value) -> json.add("\"" + key + "\":" + value));
        return json.toString();
    }

    private static String quoted(String text) {
        return "\"" + text + "\"";
    }
}
