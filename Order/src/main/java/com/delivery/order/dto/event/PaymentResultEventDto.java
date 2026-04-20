package com.delivery.order.dto.event;

import java.time.LocalDateTime;

public record PaymentResultEventDto(
    String eventId,
    String eventType,
    Integer schemaVersion,
    LocalDateTime occurredAt,
    Long orderId,
    Long paymentId,
    String paymentStatus,
    Integer finalAmount,
    Integer usedPointAmount,
    String failReason
) {
}
