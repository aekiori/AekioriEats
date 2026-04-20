package com.delivery.store.dto.event;

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
    public static StoreOrderDecisionEventDto accepted(
        String eventId,
        String eventType,
        Long orderId,
        Long storeId,
        LocalDateTime occurredAt
    ) {
        return new StoreOrderDecisionEventDto(
            eventId,
            eventType,
            1,
            occurredAt,
            orderId,
            storeId,
            "ACCEPTED",
            null
        );
    }

    public static StoreOrderDecisionEventDto rejected(
        String eventId,
        String eventType,
        Long orderId,
        Long storeId,
        String rejectReason,
        LocalDateTime occurredAt
    ) {
        return new StoreOrderDecisionEventDto(
            eventId,
            eventType,
            1,
            occurredAt,
            orderId,
            storeId,
            "REJECTED",
            rejectReason
        );
    }
}
