package com.delivery.store.dto.response;

import java.util.List;

public record ReplaceMenuTagsResponseDto(
    Long menuId,
    List<TagRefResponseDto> tags
) {
    public record TagRefResponseDto(
        Long id,
        String name
    ) {
    }
}
