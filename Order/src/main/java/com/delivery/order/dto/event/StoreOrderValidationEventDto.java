package com.delivery.order.dto.event;

import java.time.LocalDateTime;

public record StoreOrderValidationEventDto(
    String eventId,
    String eventType,
    Integer schemaVersion,
    LocalDateTime occurredAt,
    Long orderId,
    Long storeId,
    String validationResult,
    String rejectCode,
    String rejectReason
) {
}
