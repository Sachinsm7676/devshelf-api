package com.devshelf.api.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

/**
 * The application clock. Its zone ({@code app.timezone}) decides what "today" is when checking that a
 * published date is not in the future; timestamps are stored and returned in UTC regardless.
 * Tests replace this bean with a fixed clock.
 */
@Configuration
public class ClockConfig {

    @Bean
    Clock clock(@Value("${app.timezone}") String zone) {
        return Clock.system(ZoneId.of(zone));
    }
}
