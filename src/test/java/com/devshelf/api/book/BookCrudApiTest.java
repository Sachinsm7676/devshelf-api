package com.devshelf.api.book;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class BookCrudApiTest extends ApiTestSupport {

    @Test
    void getsOneBookWithEveryField() throws Exception {
        mvc.perform(get("/api/v1/books/clean-code-in-java"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("clean-code-in-java"))
                .andExpect(jsonPath("$.title").value("Clean Code in Java"))
                .andExpect(jsonPath("$.author").value("Ananya Rao"))
                .andExpect(jsonPath("$.category").value("Java"))
                .andExpect(jsonPath("$.priceInr").value(999.00))
                .andExpect(jsonPath("$.isbn").value(CLEAN_CODE_ISBN))
                .andExpect(jsonPath("$.publishedAt").value("2025-11-04"))
                .andExpect(jsonPath("$.description").isString())
                .andExpect(jsonPath("$.coverUrl").value("/assets/images/book-cover-clean-code-in-java.jpg"))
                .andExpect(jsonPath("$.rating").value(4.8))
                .andExpect(jsonPath("$.ratingCount").value(1284))
                .andExpect(jsonPath("$.popularity").value(100))
                .andExpect(jsonPath("$.createdAt", matchesPattern("\\d{4}-\\d{2}-\\d{2}T[\\d:.]+Z")))
                .andExpect(jsonPath("$.updatedAt", matchesPattern("\\d{4}-\\d{2}-\\d{2}T[\\d:.]+Z")));
    }

    @Test
    void keepsNullFieldsInTheResponse() throws Exception {
        mvc.perform(get("/api/v1/books/sql-tuning-patterns"))
                .andExpect(content().string(containsString("\"isbn\":null")))
                .andExpect(content().string(containsString("\"description\":null")));
    }

    @Test
    void unknownBookIs404() throws Exception {
        mvc.perform(get("/api/v1/books/no-such-book"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value("BOOK_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("This book does not exist. It may have been deleted."))
                .andExpect(jsonPath("$.fieldErrors").isEmpty());
    }

    @Test
    void createsABookWithASlugIdAndTrimmedFields() throws Exception {
        Map<String, Object> body = validBook("title", "  Rust for Java Developers  ");
        body.put("isbn", "   ");
        body.put("description", "");

        postBook(body)
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.LOCATION, "/api/v1/books/rust-for-java-developers"))
                .andExpect(jsonPath("$.id").value("rust-for-java-developers"))
                .andExpect(jsonPath("$.title").value("Rust for Java Developers"))
                .andExpect(jsonPath("$.priceInr").value(1099.5))
                .andExpect(jsonPath("$.rating").value(0.0))
                .andExpect(jsonPath("$.ratingCount").value(0))
                .andExpect(jsonPath("$.popularity").value(0))
                .andExpect(jsonPath("$.createdAt").value(NOW_JSON))
                .andExpect(jsonPath("$.updatedAt").value(NOW_JSON))
                .andExpect(content().string(containsString("\"priceInr\":1099.50")))
                .andExpect(content().string(containsString("\"isbn\":null")))
                .andExpect(content().string(containsString("\"description\":null")));

        mvc.perform(get("/api/v1/books/rust-for-java-developers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.author").value("Asha Verma"));
    }

    @Test
    void slugCollisionsGetANumberedSuffix() throws Exception {
        Map<String, Object> body = validBook("title", "Clean Code in Java");
        body.put("isbn", null);

        assertThat(createBook(body)).isEqualTo("clean-code-in-java-2");
        assertThat(createBook(body)).isEqualTo("clean-code-in-java-3");
    }

    @Test
    void slugDropsPunctuation() throws Exception {
        assertThat(createBook(validBook("title", "C++ & Rust: A Field Guide!!"))).isEqualTo("c-rust-a-field-guide");
    }

    @Test
    void duplicateIsbnOnCreateIs409() throws Exception {
        postBook(validBook("isbn", CLEAN_CODE_ISBN))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.code").value("ISBN_TAKEN"))
                .andExpect(jsonPath("$.message").value("Please correct the highlighted fields."))
                .andExpect(jsonPath("$.fieldErrors.isbn").value("Another book already uses this ISBN."));
    }

    @Test
    void duplicateIsbnOnUpdateIs409() throws Exception {
        putBook("mastering-react-19", validBook("isbn", CLEAN_CODE_ISBN))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ISBN_TAKEN"))
                .andExpect(jsonPath("$.fieldErrors.isbn").value("Another book already uses this ISBN."));
    }

    @Test
    void updateReplacesEditableFieldsAndKeepsTheRest() throws Exception {
        JsonNode before = objectMapper.readTree(
                mvc.perform(get("/api/v1/books/clean-code-in-java")).andReturn().getResponse().getContentAsString());
        assertThat(before.get("updatedAt").asText()).isNotEqualTo(NOW_JSON);

        Map<String, Object> body = validBook("title", "Clean Code in Java, 2nd Edition");
        body.put("isbn", CLEAN_CODE_ISBN); // its own ISBN is not a conflict

        putBook("clean-code-in-java", body)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("clean-code-in-java"))
                .andExpect(jsonPath("$.title").value("Clean Code in Java, 2nd Edition"))
                .andExpect(jsonPath("$.author").value("Asha Verma"))
                .andExpect(jsonPath("$.isbn").value(CLEAN_CODE_ISBN))
                .andExpect(jsonPath("$.rating").value(4.8))
                .andExpect(jsonPath("$.ratingCount").value(1284))
                .andExpect(jsonPath("$.popularity").value(100))
                .andExpect(jsonPath("$.createdAt").value(before.get("createdAt").asText()))
                .andExpect(jsonPath("$.updatedAt").value(NOW_JSON));

        mvc.perform(get("/api/v1/books/clean-code-in-java"))
                .andExpect(jsonPath("$.title").value("Clean Code in Java, 2nd Edition"));
    }

    @Test
    void updatingAMissingBookIs404() throws Exception {
        putBook("no-such-book", validBook())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("BOOK_NOT_FOUND"));
    }

    @Test
    void deleteReturns204ThenTheBookIsGone() throws Exception {
        mvc.perform(delete("/api/v1/books/sql-tuning-patterns"))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));
        mvc.perform(get("/api/v1/books/sql-tuning-patterns"))
                .andExpect(status().isNotFound());
        mvc.perform(delete("/api/v1/books/sql-tuning-patterns"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("BOOK_NOT_FOUND"));
    }

    @Test
    void unknownRouteWrongMethodAndWrongContentTypeUseTheErrorEnvelope() throws Exception {
        mvc.perform(get("/api/v1/nothing-here"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
        mvc.perform(patch("/api/v1/books/clean-code-in-java"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.code").value("METHOD_NOT_ALLOWED"));
        mvc.perform(post("/api/v1/books").contentType(MediaType.TEXT_PLAIN).content("hello"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.code").value("UNSUPPORTED_MEDIA_TYPE"));
    }

    @Test
    void corsPreflightAllowsTheFrontendOriginOnly() throws Exception {
        mvc.perform(options("/api/v1/books")
                        .header(HttpHeaders.ORIGIN, "http://localhost:3000")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:3000"));
        mvc.perform(get("/api/v1/books/clean-code-in-java").header(HttpHeaders.ORIGIN, "http://localhost:3000"))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, containsString("Location")));
        mvc.perform(options("/api/v1/books")
                        .header(HttpHeaders.ORIGIN, "https://evil.example")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "DELETE"))
                .andExpect(status().isForbidden());
    }

    @Test
    void healthIsUp() throws Exception {
        mvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }
}
