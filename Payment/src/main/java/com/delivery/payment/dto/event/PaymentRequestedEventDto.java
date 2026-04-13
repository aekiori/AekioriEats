package com.delivery.payment.dto.event;

import java.time.LocalDateTime;

public record PaymentRequestedEventDto(
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
