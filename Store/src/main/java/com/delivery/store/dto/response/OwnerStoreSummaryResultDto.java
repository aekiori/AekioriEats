package com.delivery.store.dto.response;

import com.delivery.store.domain.store.Store;

import java.time.LocalDateTime;

public record OwnerStoreSummaryResultDto(
    Long storeId,
    String name,
    Store.Status status,
    int minOrderAmount,
    int deliveryTip,
    String storeLogoUrl,
    LocalDateTime createdAt
) {
    public static OwnerStoreSummaryResultDto from(Store store) {
        return new OwnerStoreSummaryResultDto(
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
