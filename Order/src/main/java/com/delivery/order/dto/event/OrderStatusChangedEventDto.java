package com.delivery.order.dto.event;

import java.time.LocalDateTime;

public record OrderStatusChangedEventDto(
    String eventId,
    String eventType,
    LocalDateTime occurredAt,
    Long orderId,
    Long userId,
    Long storeId,
    String currentStatus,
    String targetStatus,
    String reason
) {
}
