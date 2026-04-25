package com.delivery.store.dto.request.owner;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record ReplaceStoreHoursRequestDto(
    @NotNull @Valid List<StoreHourRequestDto> weeklyHours
) {
}
