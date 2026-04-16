package com.delivery.order.dto.event;

import com.delivery.order.constant.OrderEventType;
import com.delivery.order.domain.order.event.OrderStatusChangedOutboxEvent;

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
    private static final Integer SCHEMA_VERSION = 1;

    public static PaymentRequestedEventDto from(
        OrderStatusChangedOutboxEvent event,
        String eventId,
        LocalDateTime occurredAt
    ) {
        return new PaymentRequestedEventDto(
            eventId,
            OrderEventType.PAYMENT_REQUESTED,
            SCHEMA_VERSION,
            occurredAt,
            event.orderId(),
            event.userId(),
            event.storeId(),
            event.totalAmount(),
            event.usedPointAmount(),
            event.finalAmount(),
            event.targetStatus().name()
        );
    }
}
