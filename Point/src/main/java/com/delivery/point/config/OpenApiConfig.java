package com.delivery.point.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI pointOpenAPI() {
        return new OpenAPI()
            .info(
                new Info()
                    .title("AekioriEats Point API")
                    .description("Point domain balance/charge APIs")
                    .version("v1")
            );
    }
}
