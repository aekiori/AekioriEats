package com.delivery.store.dto.response;

import com.delivery.store.domain.menu.Menu;

public record MenuResultDto(
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
    public static MenuResultDto from(Menu menu) {
        return new MenuResultDto(
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
