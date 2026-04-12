package com.delivery.store.dto.request;

import com.delivery.store.domain.store.Store;
import jakarta.validation.constraints.NotNull;

public record UpdateStoreStatusDto(
    @NotNull Store.Status status
) {
}
