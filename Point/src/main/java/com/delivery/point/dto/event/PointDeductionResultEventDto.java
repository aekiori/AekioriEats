package com.delivery.point.dto.event;

import java.time.LocalDateTime;

public record PointDeductionResultEventDto(
    String eventId,
    String eventType,
    Integer schemaVersion,
    LocalDateTime occurredAt,
    Long orderId,
    Long paymentId,
    Long userId,
    Integer amount,
    String reason
) {
    private static final Integer SCHEMA_VERSION = 1;

    public static PointDeductionResultEventDto deducted(
        String eventId,
        String eventType,
        Long orderId,
        Long paymentId,
        Long userId,
        Integer amount,
        LocalDateTime occurredAt
    ) {
        return new PointDeductionResultEventDto(
            eventId,
            eventType,
            SCHEMA_VERSION,
            occurredAt,
            orderId,
            paymentId,
            userId,
            amount,
            null
        );
    }

    public static PointDeductionResultEventDto failed(
        String eventId,
        String eventType,
        Long orderId,
        Long paymentId,
        Long userId,
        Integer amount,
        String reason,
        LocalDateTime occurredAt
    ) {
        return new PointDeductionResultEventDto(
            eventId,
            eventType,
            SCHEMA_VERSION,
            occurredAt,
            orderId,
            paymentId,
            userId,
            amount,
            reason
        );
    }
}
