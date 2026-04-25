package com.delivery.store.dto.request;

import com.delivery.store.domain.store.Store;
import jakarta.validation.constraints.NotNull;

public record UpdateStoreStatusRequestDto(
    @NotNull Store.Status status
) {
}
