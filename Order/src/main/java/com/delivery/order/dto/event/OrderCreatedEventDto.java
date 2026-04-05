package com.delivery.order.dto.event;

import com.delivery.order.constant.OrderEventType;
import com.delivery.order.domain.order.OrderItem;
import com.delivery.order.domain.order.event.OrderCreatedOutboxEvent;

import java.time.LocalDateTime;
import java.util.List;

public record OrderCreatedEventDto(
    String eventId,
    String eventType,
    LocalDateTime occurredAt,
    Long orderId,
    Long userId,
    Long storeId,
    Integer totalAmount,
    Integer usedPointAmount,
    Integer finalAmount,
    String status,
    List<OrderCreatedItemDto> items
) {
    public static OrderCreatedEventDto from(
        OrderCreatedOutboxEvent event,
        String eventId,
        LocalDateTime occurredAt
    ) {
        return new OrderCreatedEventDto(
            eventId,
            OrderEventType.ORDER_CREATED,
            occurredAt,
            event.orderId(),
            event.userId(),
            event.storeId(),
            event.totalAmount(),
            event.usedPointAmount(),
            event.finalAmount(),
            event.status().name(),
            event.items().stream()
                .map(OrderCreatedItemDto::from)
                .toList()
        );
    }

    public record OrderCreatedItemDto(
        Long menuId,
        String menuName,
        Integer unitPrice,
        Integer quantity,
        Integer lineAmount
    ) {
        public static OrderCreatedItemDto from(OrderItem item) {
            return new OrderCreatedItemDto(
                item.getMenuId(),
                item.getMenuName(),
                item.getUnitPrice(),
                item.getQuantity(),
                item.getLineAmount()
            );
        }
    }
}
