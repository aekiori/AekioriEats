package com.delivery.order.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record CreateOrderDto(
    @NotNull Long userId,
    @NotNull Long storeId,
    @NotBlank String deliveryAddress,
    @NotNull @Min(0) Integer usedPointAmount,
    @NotEmpty List<@Valid CreateOrderItemDto> items
) {
}
