package com.delivery.store.dto.request.owner;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record DecideStoreOrderRequestDto(
    @NotNull Decision decision,
    @Size(max = 200) String rejectReason
) {
    public enum Decision {ACCEPTED, REJECTED}
}
