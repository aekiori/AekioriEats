package com.delivery.store.dto.request.owner;

import jakarta.validation.constraints.Min;

public record DeliveryPolicyRequest(
    @Min(0) int minOrderAmount,
    @Min(0) int deliveryTip
) {
}
