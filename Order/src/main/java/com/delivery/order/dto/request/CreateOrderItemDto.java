package com.delivery.order.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateOrderItemDto(
    @NotNull Long menuId,
    @NotBlank String menuName,
    @NotNull @Min(1) Integer unitPrice,
    @NotNull @Min(1) Integer quantity
) {
}
