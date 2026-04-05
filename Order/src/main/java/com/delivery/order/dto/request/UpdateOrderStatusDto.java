package com.delivery.order.dto.request;

import com.delivery.order.domain.order.Order;
import jakarta.validation.constraints.NotNull;

public record UpdateOrderStatusDto(
    @NotNull Order.Status status,
    String reason
) {
}
