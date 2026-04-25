package com.delivery.order.service.idempotency;

import com.delivery.order.dto.response.CreateOrderResponseDto;

public interface OrderIdempotencyCacheService {
    CreateOrderResponseDto getCompletedResult(String idempotencyKey);

    String getProcessingRequestHash(String idempotencyKey);

    boolean tryAcquire(String idempotencyKey, String requestHash);

    void saveCompletedResult(String idempotencyKey, CreateOrderResponseDto result);

    void release(String idempotencyKey);
}
