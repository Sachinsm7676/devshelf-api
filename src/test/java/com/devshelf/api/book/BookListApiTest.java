package com.devshelf.api.book;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class BookListApiTest extends ApiTestSupport {

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void defaultsToFirstPageOfEightByPopularity() throws Exception {
        mvc.perform(get("/api/v1/books"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(24))
                .andExpect(jsonPath("$.totalPages").value(3))
                .andExpect(jsonPath("$.pageNumber").value(1))
                .andExpect(jsonPath("$.size").value(8))
                .andExpect(jsonPath("$.list", hasSize(8)))
                .andExpect(jsonPath("$.list[0].id").value("clean-code-in-java"))
                .andExpect(jsonPath("$.list[0].category").value("Java"))
                .andExpect(jsonPath("$.list[7].id").value("postgresql-performance-field-guide"))
                .andExpect(content().string(containsString("\"priceInr\":999.00")));
    }

    @Test
    void searchMatchesTitleCaseInsensitivelyAndTrimsTheQuery() throws Exception {
        mvc.perform(get("/api/v1/books").param("q", "  REACT  "))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.list[*].title", everyItem(containsString("React"))));
    }

    @Test
    void searchMatchesAuthor() throws Exception {
        mvc.perform(get("/api/v1/books").param("q", "vikram"))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.list[*].author", everyItem(is("Vikram Shah"))));
    }

    @Test
    void searchMatchesIsbn() throws Exception {
        mvc.perform(get("/api/v1/books").param("q", "9789350000038"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.list[0].id").value("system-design-interview-handbook"));
    }

    @Test
    void searchTreatsLikeWildcardsAndTheEscapeCharacterLiterally() throws Exception {
        String id = createBook(validBook("title", "100% Test Coverage"));

        mvc.perform(get("/api/v1/books").param("q", "%"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.list[0].id").value(id));
        mvc.perform(get("/api/v1/books").param("q", "_"))
                .andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(get("/api/v1/books").param("q", "!"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @ParameterizedTest
    @CsvSource({"DevOps, 4", "System Design, 3", "ai/ml, 3", "All, 24"})
    void filtersByCategoryLabel(String category, int expected) throws Exception {
        mvc.perform(get("/api/v1/books").param("category", category).param("size", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(expected));
    }

    @Test
    void categoryAndSearchCombine() throws Exception {
        mvc.perform(get("/api/v1/books").param("category", "DevOps").param("q", "kubernetes"))
                .andExpect(jsonPath("$.totalElements").value(4))
                .andExpect(jsonPath("$.list[*].category", everyItem(is("DevOps"))));
    }

    @ParameterizedTest(name = "sort={0}")
    @CsvSource({
            "popular,    clean-code-in-java,                       mastering-react-19",
            "newest,     practical-retrieval-augmented-generation, docker-kubernetes-in-practice-security",
            "price-asc,  clean-code-in-java-workbook,              postgresql-performance-field-guide-indexing",
            "price-desc, building-reliable-llm-applications,       system-design-interview-handbook",
            "rating,     system-design-interview-handbook,         clean-code-in-java",
            "title,      building-reliable-llm-applications,       building-reliable-llm-applications-evaluation"})
    void sortsAsDocumented(String sort, String first, String second) throws Exception {
        mvc.perform(get("/api/v1/books").param("sort", sort))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.list[0].id").value(first))
                .andExpect(jsonPath("$.list[1].id").value(second));
    }

    @Test
    void newestPutsBooksWithoutAPublishedDateLast() throws Exception {
        String id = createBook(validBook("publishedAt", null));

        mvc.perform(get("/api/v1/books").param("sort", "newest").param("size", "50"))
                .andExpect(jsonPath("$.list", hasSize(25)))
                .andExpect(jsonPath("$.list[0].id", not(is(id))))
                .andExpect(jsonPath("$.list[24].id").value(id));
    }

    @Test
    void updatedSortsByLastChangeThenById() throws Exception {
        jdbc.update("update book set updated_at = TIMESTAMP WITH TIME ZONE '2099-01-01 00:00:00+00' where id = ?",
                "sql-tuning-patterns");

        mvc.perform(get("/api/v1/books").param("sort", "updated"))
                .andExpect(jsonPath("$.list[0].id").value("sql-tuning-patterns"))
                .andExpect(jsonPath("$.list[1].id").value("building-reliable-llm-applications"));
    }

    @Test
    void returnsTheRequestedPage() throws Exception {
        mvc.perform(get("/api/v1/books").param("page", "3"))
                .andExpect(jsonPath("$.pageNumber").value(3))
                .andExpect(jsonPath("$.list", hasSize(8)))
                .andExpect(jsonPath("$.list[0].id").value("clean-code-in-java-refactoring-katas"));
    }

    @Test
    void clampsAPageBeyondTheEndToTheLastPage() throws Exception {
        mvc.perform(get("/api/v1/books").param("page", "99").param("size", "10"))
                .andExpect(jsonPath("$.pageNumber").value(3))
                .andExpect(jsonPath("$.totalPages").value(3))
                .andExpect(jsonPath("$.list", hasSize(4)))
                .andExpect(jsonPath("$.list[3].id").value("sql-tuning-patterns"));
    }

    @Test
    void emptyResultIsPageOneOfOne() throws Exception {
        mvc.perform(get("/api/v1/books").param("q", "no such book").param("page", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.list", hasSize(0)))
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.pageNumber").value(1));
    }

    @Test
    void rejectsEveryInvalidParameterAtOnce() throws Exception {
        mvc.perform(get("/api/v1/books")
                        .param("page", "0").param("size", "51").param("sort", "cheapest").param("category", "Rust"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(fieldErrorsAre(Map.of(
                        "category", "Choose a category from the list.",
                        "sort", "Sort must be one of: popular, newest, price-asc, price-desc, rating, title, updated.",
                        "page", "Page must be a whole number of 1 or more.",
                        "size", "Size must be a whole number from 1 to 50.")));
    }

    @Test
    void rejectsNonNumericPagingAndOverlongSearch() throws Exception {
        mvc.perform(get("/api/v1/books").param("page", "abc").param("size", "x").param("q", "q".repeat(101)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.page").value("Page must be a whole number of 1 or more."))
                .andExpect(jsonPath("$.fieldErrors.size").value("Size must be a whole number from 1 to 50."))
                .andExpect(jsonPath("$.fieldErrors.q").value("Search text can be at most 100 characters."));
    }
}
