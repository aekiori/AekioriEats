package com.delivery.order.dto.response;

import com.delivery.order.domain.order.Order;

import java.time.LocalDateTime;

public record OrderSummaryResponseDto(
    Long orderId,
    Long storeId,
    String status,
    Integer finalAmount,
    LocalDateTime createdAt
) {
    public static OrderSummaryResponseDto from(Order order) {
        return new OrderSummaryResponseDto(
            order.getId(),
            order.getStoreId(),
            order.getStatus().name(),
            order.getFinalAmount(),
            order.getCreatedAt()
        );
    }
}
