package com.delivery.store.dto.request.owner;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateMenuRequest(
    Long menuGroupId,
    @NotBlank @Size(max = 100) String name,
    @Size(max = 500) String description,
    @Min(0) int price,
    Boolean isAvailable,
    @Size(max = 512) String imageUrl,
    @Min(0) Integer displayOrder
) {
    public boolean isAvailableOrDefault() {
        return isAvailable == null || isAvailable;
    }

    public int displayOrderOrDefault() {
        return displayOrder == null ? 0 : displayOrder;
    }
}
