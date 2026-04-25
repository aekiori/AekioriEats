package com.delivery.order.dto.response;

import com.delivery.order.domain.order.Order;

import java.time.LocalDateTime;

public record CreateOrderResponseDto(
    Long orderId,
    Long userId,
    Long storeId,
    String status,
    String deliveryAddress,
    Integer totalAmount,
    Integer usedPointAmount,
    Integer finalAmount,
    LocalDateTime createdAt
) {
    public static CreateOrderResponseDto from(Order order) {
        return new CreateOrderResponseDto(
            order.getId(),
            order.getUserId(),
            order.getStoreId(),
            order.getStatus().name(),
            order.getDeliveryAddress(),
            order.getTotalAmount(),
            order.getUsedPointAmount(),
            order.getFinalAmount(),
            order.getCreatedAt()
        );
    }
}
