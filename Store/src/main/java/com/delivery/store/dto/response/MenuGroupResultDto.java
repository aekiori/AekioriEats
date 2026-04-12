package com.delivery.store.dto.response;

import com.delivery.store.domain.menu.MenuGroup;

public record MenuGroupResultDto(
    Long id,
    String name
) {
    public static MenuGroupResultDto from(MenuGroup menuGroup) {
        return new MenuGroupResultDto(menuGroup.getId(), menuGroup.getName());
    }
}
