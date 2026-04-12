package com.delivery.store.dto.request.owner;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record ReplaceStoreHolidaysRequest(
    @NotNull @Valid List<StoreHolidayRequest> holidays
) {
}
