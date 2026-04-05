package com.delivery.order.dto.response;

import com.delivery.order.domain.order.Order;

import java.time.LocalDateTime;

public record UpdateOrderStatusResultDto(
    Long orderId,
    String status,
    LocalDateTime updatedAt
) {
    public static UpdateOrderStatusResultDto from(Order order) {
        return new UpdateOrderStatusResultDto(
            order.getId(),
            order.getStatus().name(),
            order.getUpdatedAt()
        );
    }
}
