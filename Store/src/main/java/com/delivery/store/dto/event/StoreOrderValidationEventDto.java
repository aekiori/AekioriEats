package com.delivery.store.dto.event;

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
    private static final Integer SCHEMA_VERSION = 1;

    public static StoreOrderValidationEventDto accepted(
        String eventId,
        String eventType,
        Long orderId,
        Long storeId,
        LocalDateTime occurredAt
    ) {
        return new StoreOrderValidationEventDto(
            eventId,
            eventType,
            SCHEMA_VERSION,
            occurredAt,
            orderId,
            storeId,
            "ACCEPTED",
            null,
            null
        );
    }

    public static StoreOrderValidationEventDto rejected(
        String eventId,
        String eventType,
        Long orderId,
        Long storeId,
        String rejectCode,
        String rejectReason,
        LocalDateTime occurredAt
    ) {
        return new StoreOrderValidationEventDto(
            eventId,
            eventType,
            SCHEMA_VERSION,
            occurredAt,
            orderId,
            storeId,
            "REJECTED",
            rejectCode,
            rejectReason
        );
    }
}
