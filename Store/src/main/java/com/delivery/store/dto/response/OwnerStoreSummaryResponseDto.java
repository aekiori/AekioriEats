package com.delivery.store.dto.response;

import com.delivery.store.domain.store.Store;

import java.time.LocalDateTime;

public record OwnerStoreSummaryResponseDto(
    Long storeId,
    String name,
    Store.Status status,
    int minOrderAmount,
    int deliveryTip,
    String storeLogoUrl,
    LocalDateTime createdAt
) {
    public static OwnerStoreSummaryResponseDto from(Store store) {
        return new OwnerStoreSummaryResponseDto(
            store.getId(),
            store.getName(),
            store.getStatus(),
            store.getMinOrderAmount(),
            store.getDeliveryTip(),
            store.getStoreLogoUrl(),
            store.getCreatedAt()
        );
    }
}
