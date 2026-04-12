package com.delivery.store.dto.request.owner;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record UpdateOwnerStoreRequest(
    @NotBlank @Size(max = 100) String name,
    List<Long> categoryIds,
    @Valid DeliveryPolicyRequest deliveryPolicy,
    @Size(max = 512) String storeLogoUrl
) {
}
