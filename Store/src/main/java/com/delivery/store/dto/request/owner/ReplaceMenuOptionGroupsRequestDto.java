package com.delivery.store.dto.request.owner;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record ReplaceMenuOptionGroupsRequestDto(
    @NotNull @Valid List<OptionGroupRequest> optionGroups
) {
    public record OptionGroupRequest(
        @NotBlank @Size(max = 100) String name,
        boolean isRequired,
        boolean isMultiple,
        @Min(0) int minSelectCount,
        @Min(0) int maxSelectCount,
        @NotNull @Valid List<OptionRequest> options
    ) {
    }

    public record OptionRequest(
        @NotBlank @Size(max = 100) String name,
        @Min(0) int extraPrice,
        boolean isAvailable
    ) {
    }
}
