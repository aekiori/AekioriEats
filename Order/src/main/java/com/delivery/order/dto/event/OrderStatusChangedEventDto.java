package com.delivery.order.dto.event;

import com.delivery.order.constant.OrderEventType;
import com.delivery.order.domain.order.event.OrderStatusChangedOutboxEvent;

import java.time.LocalDateTime;

public record OrderStatusChangedEventDto(
    String eventId,
    String eventType,
    LocalDateTime occurredAt,
    Long orderId,
    Long userId,
    Long storeId,
    String currentStatus,
    String targetStatus,
    String reason
) {
    public static OrderStatusChangedEventDto from(
        OrderStatusChangedOutboxEvent event,
        String eventId,
        LocalDateTime occurredAt
    ) {
        return new OrderStatusChangedEventDto(
            eventId,
            OrderEventType.ORDER_STATUS_CHANGED,
            occurredAt,
            event.orderId(),
            event.userId(),
            event.storeId(),
            event.currentStatus().name(),
            event.targetStatus().name(),
            event.reason()
        );
    }
}
