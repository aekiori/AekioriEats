package com.delivery.store.dto.response;

import com.delivery.store.domain.store.StoreOrder;

import java.time.LocalDateTime;

public record StoreOrderDecisionResponseDto(
    Long orderId,
    Long storeId,
    String status,
    String rejectReason,
    LocalDateTime decidedAt
) {
    public static StoreOrderDecisionResponseDto from(StoreOrder storeOrder) {
        return new StoreOrderDecisionResponseDto(
            storeOrder.getOrderId(),
            storeOrder.getStoreId(),
            storeOrder.getStatus().name(),
            storeOrder.getRejectReason(),
            storeOrder.getDecidedAt()
        );
    }
}
