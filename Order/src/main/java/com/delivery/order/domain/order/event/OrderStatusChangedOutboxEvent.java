package com.delivery.order.domain.order.event;

import com.delivery.order.domain.order.Order;

public record OrderStatusChangedOutboxEvent(
    Long orderId,
    Long userId,
    Long storeId,
    Order.Status currentStatus,
    Order.Status targetStatus,
    String reason
) {
    public static OrderStatusChangedOutboxEvent from(
        Order order,
        Order.Status currentStatus,
        Order.Status targetStatus,
        String reason
    ) {
        return new OrderStatusChangedOutboxEvent(
            order.getId(),
            order.getUserId(),
            order.getStoreId(),
            currentStatus,
            targetStatus,
            reason
        );
    }
}
