package com.delivery.store.dto.event;

import java.time.LocalDateTime;

public record OrderCreatedEventDto(
    String eventId,
    String eventType,
    Integer schemaVersion,
    LocalDateTime occurredAt,
    Long orderId,
    Long userId,
    Long storeId,
    Integer totalAmount,
    Integer usedPointAmount,
    Integer finalAmount,
    String status
) {
}
