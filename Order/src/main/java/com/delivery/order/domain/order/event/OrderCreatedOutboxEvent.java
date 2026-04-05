package com.delivery.order.domain.order.event;

import com.delivery.order.domain.order.Order;
import com.delivery.order.domain.order.OrderItem;

import java.util.List;

public record OrderCreatedOutboxEvent(
    Long orderId,
    Long userId,
    Long storeId,
    Integer totalAmount,
    Integer usedPointAmount,
    Integer finalAmount,
    Order.Status status,
    List<OrderItem> items
) {
    public static OrderCreatedOutboxEvent from(Order order,List<OrderItem> items) {
        return new OrderCreatedOutboxEvent(
            order.getId(),
            order.getUserId(),
            order.getStoreId(),
            order.getTotalAmount(),
            order.getUsedPointAmount(),
            order.getFinalAmount(),
            order.getStatus(),
            items
        );
    }
}
