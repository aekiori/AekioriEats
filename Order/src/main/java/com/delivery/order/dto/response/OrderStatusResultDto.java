package com.delivery.order.dto.response;

import com.delivery.order.domain.order.Order;

public record OrderStatusResultDto(Long orderId, String status) {
    public static OrderStatusResultDto from(Order order) {
        return new OrderStatusResultDto(order.getId(), order.getStatus().name());
    }
}
