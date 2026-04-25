package com.delivery.order.dto.response;

import com.delivery.order.domain.order.Order;

public record OrderStatusResponseDto(Long orderId, String status) {
    public static OrderStatusResponseDto from(Order order) {
        return new OrderStatusResponseDto(order.getId(), order.getStatus().name());
    }
}
