package com.delivery.store.domain.menu;

import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@EqualsAndHashCode
@NoArgsConstructor
public class MenuTagId implements Serializable {
    private Long menuId;
    private Long tagId;

    public MenuTagId(Long menuId, Long tagId) {
        this.menuId = menuId;
        this.tagId = tagId;
    }
}
