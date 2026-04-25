package com.delivery.store.dto.request.owner;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateMenuGroupRequestDto(
    @NotBlank @Size(max = 100) String name,
    @Min(0) int displayOrder
) {
}
