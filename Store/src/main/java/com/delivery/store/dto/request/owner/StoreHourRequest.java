package com.delivery.store.dto.request.owner;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;

public record StoreHourRequest(
    @Min(1) @Max(7) int dayOfWeek,
    @Pattern(regexp = "^([01][0-9]|2[0-3]):[0-5][0-9]:[0-5][0-9]$") String openTime,
    @Pattern(regexp = "^([01][0-9]|2[0-3]):[0-5][0-9]:[0-5][0-9]$") String closeTime
) {
}
