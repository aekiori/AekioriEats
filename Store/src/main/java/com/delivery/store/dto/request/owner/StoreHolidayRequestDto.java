package com.delivery.store.dto.request.owner;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record StoreHolidayRequestDto(
    @NotNull LocalDate date,
    @Size(max = 100) String reason
) {
}
