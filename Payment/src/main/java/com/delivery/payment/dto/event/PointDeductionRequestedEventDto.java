package com.delivery.payment.dto.event;

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
    private static final Integer SCHEMA_VERSION = 1;

    public static PointDeductionRequestedEventDto from(
        String eventId,
        String eventType,
        Long orderId,
        Long paymentId,
        Long userId,
        Integer amount,
        LocalDateTime occurredAt
    ) {
        return new PointDeductionRequestedEventDto(
            eventId,
            eventType,
            SCHEMA_VERSION,
            occurredAt,
            orderId,
            paymentId,
            userId,
            amount
        );
    }
}
