package com.devshelf.api.book;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class SlugGeneratorTest {

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "Clean Code in Java                  | clean-code-in-java",
            "Docker & Kubernetes in Practice     | docker-kubernetes-in-practice",
            "System Design Interview Handbook, Vol. 2 | system-design-interview-handbook-vol-2",
            "'  --Hello,   World!--  '           | hello-world",
            "C++ & Rust: A Field Guide!!         | c-rust-a-field-guide",
            "Café Crème Brûlée                   | cafe-creme-brulee",
            "100% Test Coverage                  | 100-test-coverage",
            "시카 리페어                          | book",
            "!!                                  | book"})
    void slugifiesTitles(String title, String expected) {
        assertThat(SlugGenerator.slugify(title)).isEqualTo(expected);
    }

    @Test
    void cutsTo120CharactersWithoutATrailingHyphen() {
        String title = "a".repeat(119) + " bcdef";

        String slug = SlugGenerator.slugify(title);

        assertThat(slug).hasSize(119).isEqualTo("a".repeat(119));
        assertThat(SlugGenerator.slugify("x".repeat(200))).hasSize(SlugGenerator.MAX_LENGTH);
    }

    @Test
    void firstFreeAppendsTheFirstUnusedNumber() {
        Set<String> taken = Set.of("clean-code", "clean-code-2", "clean-code-3", "clean-code-workbook");

        assertThat(SlugGenerator.firstFree("clean-code", taken::contains)).isEqualTo("clean-code-4");
        assertThat(SlugGenerator.firstFree("rust", taken::contains)).isEqualTo("rust");
    }
}
