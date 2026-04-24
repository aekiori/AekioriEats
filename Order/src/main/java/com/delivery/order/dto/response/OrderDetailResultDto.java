package com.delivery.order.dto.response;

import com.delivery.order.domain.order.Order;

import java.time.LocalDateTime;
import java.util.List;

public record OrderDetailResultDto(
    Long orderId,
    Long userId,
    Long storeId,
    String status,
    String deliveryAddress,
    Integer totalAmount,
    Integer usedPointAmount,
    Integer finalAmount,
    List<OrderItemResultDto> items,
    List<OrderStatusHistoryResultDto> statusHistories,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    public static OrderDetailResultDto from(
        Order order,
        List<OrderItemResultDto> items,
        List<OrderStatusHistoryResultDto> statusHistories
    ) {
        return new OrderDetailResultDto(
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
