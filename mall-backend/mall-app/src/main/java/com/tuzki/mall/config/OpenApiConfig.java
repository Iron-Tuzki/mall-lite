package com.tuzki.mall.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI configuration for generating Swagger UI documentation of mall-lite REST APIs.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI mallLiteOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Mall Lite API")
                        .version("0.0.1")
                        .description("REST API documentation for the mall-lite learning project."));
    }
}
