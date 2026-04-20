package com.delivery.payment.dto.event;

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
    private static final Integer SCHEMA_VERSION = 1;

    public static PaymentResultEventDto succeeded(
        String eventId,
        String eventType,
        Long orderId,
        Long paymentId,
        Integer finalAmount,
        Integer usedPointAmount,
        LocalDateTime occurredAt
    ) {
        return new PaymentResultEventDto(
            eventId,
            eventType,
            SCHEMA_VERSION,
            occurredAt,
            orderId,
            paymentId,
            "SUCCEEDED",
            finalAmount,
            usedPointAmount,
            null
        );
    }

    public static PaymentResultEventDto failed(
        String eventId,
        String eventType,
        Long orderId,
        Long paymentId,
        String failReason,
        LocalDateTime occurredAt
    ) {
        return new PaymentResultEventDto(
            eventId,
            eventType,
            SCHEMA_VERSION,
            occurredAt,
            orderId,
            paymentId,
            "FAILED",
            null,
            null,
            failReason
        );
    }

    public static PaymentResultEventDto refunded(
        String eventId,
        String eventType,
        Long orderId,
        Long paymentId,
        Integer finalAmount,
        String refundReason,
        LocalDateTime occurredAt
    ) {
        return new PaymentResultEventDto(
            eventId,
            eventType,
            SCHEMA_VERSION,
            occurredAt,
            orderId,
            paymentId,
            "REFUNDED",
            finalAmount,
            null,
            refundReason
        );
    }
}
