package com.delivery.store.dto.response;

import java.util.List;

public record ReplaceMenuTagsResponseDto(
    Long menuId,
    List<TagRefDto> tags
) {
    public record TagRefDto(
        Long id,
        String name
    ) {
    }
}
