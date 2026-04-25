package com.delivery.store.dto.response;

import com.delivery.store.domain.menu.Menu;

public record MenuResponseDto(
    Long id,
    Long storeId,
    Long menuGroupId,
    String name,
    String description,
    int price,
    boolean isAvailable,
    boolean isSoldOut,
    String imageUrl
) {
    public static MenuResponseDto from(Menu menu) {
        return new MenuResponseDto(
            menu.getId(),
            menu.getStore().getId(),
            menu.getMenuGroup() != null ? menu.getMenuGroup().getId() : null,
            menu.getName(),
            menu.getDescription(),
            menu.getPrice(),
            menu.isAvailable(),
            menu.isSoldOut(),
            menu.getMenuImageUrl()
        );
    }
}
