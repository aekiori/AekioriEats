package com.delivery.order.service;

import com.delivery.order.dto.response.CreateOrderResultDto;

public interface OrderIdempotencyCacheService {
    CreateOrderResultDto getCompletedResult(String idempotencyKey);

    String getProcessingRequestHash(String idempotencyKey);

    boolean tryAcquire(String idempotencyKey, String requestHash);

    void saveCompletedResult(String idempotencyKey, CreateOrderResultDto result);

    void release(String idempotencyKey);
}
