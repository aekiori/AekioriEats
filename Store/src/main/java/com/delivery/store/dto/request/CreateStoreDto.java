package com.delivery.store.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreateStoreDto(
    @NotNull @Positive Long ownerUserId,
    @NotBlank @Size(max = 100) String name
) {
}
