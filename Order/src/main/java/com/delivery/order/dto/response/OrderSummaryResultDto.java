package com.delivery.order.dto.response;

import com.delivery.order.domain.order.Order;

import java.time.LocalDateTime;

public record OrderSummaryResultDto(
    Long orderId,
    Long storeId,
    String status,
    Integer finalAmount,
    LocalDateTime createdAt
) {
    public static OrderSummaryResultDto from(Order order) {
        return new OrderSummaryResultDto(
            order.getId(),
            order.getStoreId(),
            order.getStatus().name(),
            order.getFinalAmount(),
            order.getCreatedAt()
        );
    }
}
