package com.delivery.store.dto.response;

import com.delivery.store.domain.store.Store;

import java.time.LocalDateTime;

public record StoreDetailResponseDto(
    Long storeId,
    Long ownerUserId,
    String name,
    Store.Status status,
    boolean statusOverride,
    int minOrderAmount,
    int deliveryTip,
    String storeLogoUrl,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    public static StoreDetailResponseDto from(Store store) {
        return new StoreDetailResponseDto(
            store.getId(),
            store.getOwnerUserId(),
            store.getName(),
            store.getStatus(),
            store.isStatusOverride(),
            store.getMinOrderAmount(),
            store.getDeliveryTip(),
            store.getStoreLogoUrl(),
            store.getCreatedAt(),
            store.getUpdatedAt()
        );
    }
}
