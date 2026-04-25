package com.delivery.store.dto.response;

import com.delivery.store.domain.store.StoreOrder;

import java.time.LocalDateTime;

public record StoreOrderResponseDto(
    Long orderId,
    Long storeId,
    Long userId,
    Integer finalAmount,
    String status,
    String rejectReason,
    LocalDateTime paidAt,
    LocalDateTime decidedAt,
    LocalDateTime createdAt
) {
    public static StoreOrderResponseDto from(StoreOrder storeOrder) {
        return new StoreOrderResponseDto(
            storeOrder.getOrderId(),
            storeOrder.getStoreId(),
            storeOrder.getUserId(),
            storeOrder.getFinalAmount(),
            storeOrder.getStatus().name(),
            storeOrder.getRejectReason(),
            storeOrder.getPaidAt(),
            storeOrder.getDecidedAt(),
            storeOrder.getCreatedAt()
        );
    }
}
