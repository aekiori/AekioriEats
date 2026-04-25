package com.delivery.order;

import com.delivery.order.dto.response.CreateOrderResponseDto;
import com.delivery.order.service.idempotency.OrderIdempotencyCacheService;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration
public class TestIdempotencyCacheConfig {
    @Bean
    OrderIdempotencyCacheService orderIdempotencyCacheService() {
        return new OrderIdempotencyCacheService() {
            @Override
            public CreateOrderResponseDto getCompletedResult(String idempotencyKey) {
                return null;
            }

            @Override
            public String getProcessingRequestHash(String idempotencyKey) {
                return null;
            }

            @Override
            public boolean tryAcquire(String idempotencyKey, String requestHash) {
                return true;
            }

            @Override
            public void saveCompletedResult(String idempotencyKey, CreateOrderResponseDto result) {
            }

            @Override
            public void release(String idempotencyKey) {
            }
        };
    }
}
