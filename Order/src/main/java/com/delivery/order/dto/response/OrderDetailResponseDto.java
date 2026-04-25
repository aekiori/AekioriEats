package com.delivery.order.dto.response;

import com.delivery.order.domain.order.Order;

import java.time.LocalDateTime;
import java.util.List;

public record OrderDetailResponseDto(
    Long orderId,
    Long userId,
    Long storeId,
    String status,
    String deliveryAddress,
    Integer totalAmount,
    Integer usedPointAmount,
    Integer finalAmount,
    List<OrderItemResponseDto> items,
    List<OrderStatusHistoryResponseDto> statusHistories,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    public static OrderDetailResponseDto from(
        Order order,
        List<OrderItemResponseDto> items,
        List<OrderStatusHistoryResponseDto> statusHistories
    ) {
        return new OrderDetailResponseDto(
            order.getId(),
            order.getUserId(),
            order.getStoreId(),
            order.getStatus().name(),
            order.getDeliveryAddress(),
            order.getTotalAmount(),
            order.getUsedPointAmount(),
            order.getFinalAmount(),
            items,
            statusHistories,
            order.getCreatedAt(),
            order.getUpdatedAt()
        );
    }
}
