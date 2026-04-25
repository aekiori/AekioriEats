package com.delivery.store.dto.response;

import com.delivery.store.domain.menu.MenuGroup;

public record MenuGroupResponseDto(
    Long id,
    String name
) {
    public static MenuGroupResponseDto from(MenuGroup menuGroup) {
        return new MenuGroupResponseDto(menuGroup.getId(), menuGroup.getName());
    }
}
