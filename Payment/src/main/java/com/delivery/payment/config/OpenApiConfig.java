package com.delivery.payment.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI paymentOpenAPI() {
        return new OpenAPI()
            .info(
                new Info()
                    .title("AekioriEats Payment API")
                    .description("Payment domain confirm/refund APIs")
                    .version("v1")
            );
    }
}
