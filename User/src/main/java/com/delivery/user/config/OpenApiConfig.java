package com.delivery.user.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI userOpenAPI() {
        return new OpenAPI()
            .info(
                new Info()
                    .title("AekioriEats User API")
                    .description("User domain create/query/status APIs")
                    .version("v1")
            );
    }
}
