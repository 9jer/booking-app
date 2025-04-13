package com.example.reviewservice.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Review Service")
                        .version("1.0")
                        .description("Review Service API")
                        .license(new License().name("Apache 2.0").url("https://springdocs.org")));
    }
}
