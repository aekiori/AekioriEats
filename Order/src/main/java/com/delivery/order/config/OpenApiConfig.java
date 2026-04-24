package com.delivery.order.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI orderOpenAPI() {
        return new OpenAPI()
            .info(
                new Info()
                    .title("AekioriEats Order API")
                    .description("Order domain create/query/status APIs")
                    .version("v1")
            );
    }
}
