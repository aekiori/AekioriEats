package com.delivery.order.domain.order.event;

import com.delivery.order.domain.order.Order;

public record OrderStatusChangedOutboxEvent(
    Long orderId,
    Long userId,
    Long storeId,
    Integer totalAmount,
    Integer usedPointAmount,
    Integer finalAmount,
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
            order.getTotalAmount(),
            order.getUsedPointAmount(),
            order.getFinalAmount(),
            currentStatus,
            targetStatus,
            reason
        );
    }
}
