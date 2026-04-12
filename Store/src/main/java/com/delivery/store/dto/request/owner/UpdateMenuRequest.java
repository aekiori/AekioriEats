package com.delivery.store.dto.request.owner;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateMenuRequest(
    Long menuGroupId,
    @NotBlank @Size(max = 100) String name,
    @Size(max = 500) String description,
    @Min(0) int price,
    boolean isAvailable,
    boolean isSoldOut,
    @Size(max = 512) String imageUrl
) {
}
