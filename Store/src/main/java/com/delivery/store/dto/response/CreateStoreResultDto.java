package com.delivery.store.dto.response;

import com.delivery.store.domain.store.Store;

import java.time.LocalDateTime;

public record CreateStoreResultDto(
    Long storeId,
    Long ownerUserId,
    String name,
    Store.Status status,
    boolean statusOverride,
    int minOrderAmount,
    int deliveryTip,
    String storeLogoUrl,
    LocalDateTime createdAt
) {
    public static CreateStoreResultDto from(Store store) {
        return new CreateStoreResultDto(
            store.getId(),
            store.getOwnerUserId(),
            store.getName(),
            store.getStatus(),
            store.isStatusOverride(),
            store.getMinOrderAmount(),
            store.getDeliveryTip(),
            store.getStoreLogoUrl(),
            store.getCreatedAt()
        );
    }
}
