package com.delivery.point.dto.event;

import java.time.LocalDateTime;

public record PointDeductionRequestedEventDto(
    String eventId,
    String eventType,
    Integer schemaVersion,
    LocalDateTime occurredAt,
    Long orderId,
    Long paymentId,
    Long userId,
    Integer amount
) {
}
