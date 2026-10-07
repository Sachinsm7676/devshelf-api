package com.devshelf.api.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;

import org.junit.jupiter.api.Test;

class DatabaseUrlEnvironmentPostProcessorTest {

    @Test
    void renderInternalUrlWithoutPortGetsTheDefaultPort() {
        Map<String, Object> properties = DatabaseUrlEnvironmentPostProcessor.toDataSourceProperties(
                "postgresql://devshelf:s3cret@dpg-abc123-a/devshelf");

        assertThat(properties)
                .containsEntry("spring.datasource.url", "jdbc:postgresql://dpg-abc123-a:5432/devshelf")
                .containsEntry("spring.datasource.username", "devshelf")
                .containsEntry("spring.datasource.password", "s3cret");
    }

    @Test
    void keepsPortAndQueryAndDecodesThePassword() {
        Map<String, Object> properties = DatabaseUrlEnvironmentPostProcessor.toDataSourceProperties(
                "postgres://app:p%40ss%3Aword@db.example.com:6543/books?sslmode=require");

        assertThat(properties)
                .containsEntry("spring.datasource.url", "jdbc:postgresql://db.example.com:6543/books?sslmode=require")
                .containsEntry("spring.datasource.username", "app")
                .containsEntry("spring.datasource.password", "p@ss:word");
    }

    @Test
    void refusesAnotherScheme() {
        assertThatThrownBy(() -> DatabaseUrlEnvironmentPostProcessor.toDataSourceProperties("mysql://u:p@h/db"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("postgresql://");
    }
}
