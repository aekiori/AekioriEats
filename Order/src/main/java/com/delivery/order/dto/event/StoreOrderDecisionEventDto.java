package com.delivery.order.dto.event;

import java.time.LocalDateTime;

public record StoreOrderDecisionEventDto(
    String eventId,
    String eventType,
    Integer schemaVersion,
    LocalDateTime occurredAt,
    Long orderId,
    Long storeId,
    String decision,
    String rejectReason
) {
}
