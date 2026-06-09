package com.aws.carddemo.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI / Swagger UI metadata for the migrated CardDemo REST API.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI cardDemoOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("CardDemo API")
                        .description("Java 21 / Spring Boot migration of the COBOL CardDemo "
                                + "mainframe credit card management application")
                        .version("1.0.0")
                        .license(new License().name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")));
    }
}
