package com.delivery.store.dto.response;

import java.util.List;

public record ReplaceMenuOptionGroupsResponseDto(
    Long menuId,
    List<OptionGroupResultDto> optionGroups
) {
    public record OptionGroupResultDto(
        String name,
        boolean isRequired,
        boolean isMultiple,
        int minSelectCount,
        int maxSelectCount,
        List<OptionResultDto> options
    ) {
    }

    public record OptionResultDto(
        String name,
        int extraPrice,
        boolean isAvailable
    ) {
    }
}
