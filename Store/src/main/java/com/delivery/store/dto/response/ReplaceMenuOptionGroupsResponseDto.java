package com.delivery.store.dto.response;

import java.util.List;

public record ReplaceMenuOptionGroupsResponseDto(
    Long menuId,
    List<OptionGroupResponseDto> optionGroups
) {
    public record OptionGroupResponseDto(
        String name,
        boolean isRequired,
        boolean isMultiple,
        int minSelectCount,
        int maxSelectCount,
        List<OptionResponseDto> options
    ) {
    }

    public record OptionResponseDto(
        String name,
        int extraPrice,
        boolean isAvailable
    ) {
    }
}
