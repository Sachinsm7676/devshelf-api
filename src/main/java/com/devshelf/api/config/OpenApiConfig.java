package com.devshelf.api.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** OpenAPI metadata. Swagger UI is served at {@code /swagger-ui.html}. */
@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI devShelfOpenApi() {
        return new OpenAPI().info(new Info()
                .title("DevShelf Books API")
                .version("v1")
                .description("Catalogue of books for developers. Every error response has the shape "
                        + "{status, code, message, fieldErrors}."));
    }
}
