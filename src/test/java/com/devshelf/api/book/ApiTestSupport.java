package com.devshelf.api.book;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/**
 * Shared setup for the API tests: H2 in PostgreSQL mode with the real Flyway migrations (so every test
 * starts from the 24 seeded books), a fixed clock, and a rollback after each test.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@Import(ApiTestSupport.FixedClock.class)
abstract class ApiTestSupport {

    /** 2026-10-07 20:00 UTC, which is already 2026-10-08 01:30 in Asia/Kolkata. */
    static final Instant NOW = Instant.parse("2026-10-07T20:00:00Z");
    static final String NOW_JSON = "2026-10-07T20:00:00Z";

    static final String CLEAN_CODE_ISBN = "9789350000014";

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper objectMapper;

    @TestConfiguration
    static class FixedClock {
        @Bean
        @Primary
        Clock fixedClock() {
            return Clock.fixed(NOW, ZoneId.of("Asia/Kolkata"));
        }
    }

    /** A request body that passes every rule; tests override single fields. */
    static Map<String, Object> validBook() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("title", "Rust for Java Developers");
        body.put("author", "Asha Verma");
        body.put("category", "Java");
        body.put("priceInr", 1099.50);
        body.put("isbn", "9789350000991");
        body.put("publishedAt", "2026-01-15");
        body.put("description", "Ownership and borrowing explained for people who think in objects.");
        body.put("coverUrl", "https://example.com/covers/rust.jpg");
        return body;
    }

    static Map<String, Object> validBook(String key, Object value) {
        Map<String, Object> body = validBook();
        body.put(key, value);
        return body;
    }

    ResultActions postBook(Object body) throws Exception {
        return mvc.perform(post("/api/v1/books").contentType(MediaType.APPLICATION_JSON).content(json(body)));
    }

    ResultActions putBook(String id, Object body) throws Exception {
        return mvc.perform(put("/api/v1/books/" + id).contentType(MediaType.APPLICATION_JSON).content(json(body)));
    }

    String json(Object body) throws Exception {
        return body instanceof String raw ? raw : objectMapper.writeValueAsString(body);
    }

    /** Asserts the response's {@code fieldErrors} is exactly {@code expected}: no missing and no extra fields. */
    ResultMatcher fieldErrorsAre(Map<String, String> expected) {
        return result -> {
            JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
            Map<String, String> actual = objectMapper.convertValue(body.get("fieldErrors"), new TypeReference<>() {
            });
            assertThat(actual).isEqualTo(expected);
        };
    }

    /** Creates a book through the API and returns its id. */
    String createBook(Map<String, Object> body) throws Exception {
        String response = postBook(body).andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asText();
    }
}
