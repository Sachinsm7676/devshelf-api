package com.devshelf.api.config;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

/**
 * Accepts the database as one {@code DATABASE_URL} in the {@code postgresql://user:password@host[:port]/database}
 * form that Render (and most hosts) hand out, and turns it into the JDBC url, username and password Spring expects.
 * Render's Blueprint can only pass a Postgres connection as that string, not as separate host and port.
 * When {@code DATABASE_URL} is not set, the {@code DB_HOST} / {@code DB_PORT} / ... variables apply as before.
 */
public class DatabaseUrlEnvironmentPostProcessor implements EnvironmentPostProcessor {

    static final String VARIABLE = "DATABASE_URL";
    private static final int DEFAULT_PORT = 5432;

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String databaseUrl = environment.getProperty(VARIABLE);
        if (databaseUrl == null || databaseUrl.isBlank()) {
            return;
        }
        environment.getPropertySources().addFirst(new MapPropertySource("databaseUrl", toDataSourceProperties(databaseUrl)));
    }

    /** {@code postgresql://u:p@host:6543/db?sslmode=require} → url {@code jdbc:postgresql://host:6543/db?sslmode=require}, u, p */
    static Map<String, Object> toDataSourceProperties(String databaseUrl) {
        URI uri = URI.create(databaseUrl.trim());
        String scheme = uri.getScheme();
        if (!"postgres".equals(scheme) && !"postgresql".equals(scheme)) {
            throw new IllegalArgumentException(VARIABLE + " must start with postgresql:// (got " + scheme + "://)");
        }
        int port = uri.getPort() == -1 ? DEFAULT_PORT : uri.getPort();
        String query = uri.getRawQuery() == null ? "" : "?" + uri.getRawQuery();

        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("spring.datasource.url", "jdbc:postgresql://" + uri.getHost() + ":" + port + uri.getRawPath() + query);
        String userInfo = uri.getRawUserInfo();
        if (userInfo != null) {
            int colon = userInfo.indexOf(':');
            properties.put("spring.datasource.username", decode(colon < 0 ? userInfo : userInfo.substring(0, colon)));
            if (colon >= 0) {
                properties.put("spring.datasource.password", decode(userInfo.substring(colon + 1)));
            }
        }
        return properties;
    }

    private static String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }
}
