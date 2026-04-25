package com.delivery.order.dto.response;

import com.delivery.order.domain.order.Order;

import java.time.LocalDateTime;

public record UpdateOrderStatusResponseDto(
    Long orderId,
    String status,
    LocalDateTime updatedAt
) {
    public static UpdateOrderStatusResponseDto from(Order order) {
        return new UpdateOrderStatusResponseDto(
            order.getId(),
            order.getStatus().name(),
            order.getUpdatedAt()
        );
    }
}
