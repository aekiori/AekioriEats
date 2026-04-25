package com.delivery.store.dto.request.owner;

import com.delivery.store.domain.store.StoreOrder;

public record GetStoreOrdersRequestDto(StoreOrder.Status status) {
    public StoreOrder.Status resolvedStatus() {
        return status == null ? StoreOrder.Status.PENDING : status;
    }
}
