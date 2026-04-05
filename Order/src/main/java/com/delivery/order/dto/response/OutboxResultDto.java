package com.delivery.order.dto.response;

import java.time.LocalDateTime;

public record OutboxResultDto(
    Long id,
    String eventId,
    String aggregateType,
    Long aggregateId,
    String eventType,
    String status,
    String partitionKey,
    LocalDateTime createdAt
) {
}
