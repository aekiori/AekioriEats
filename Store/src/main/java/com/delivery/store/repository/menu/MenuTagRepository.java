package com.delivery.store.repository.menu;

import com.delivery.store.domain.menu.MenuTag;
import com.delivery.store.domain.menu.MenuTagId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface MenuTagRepository extends JpaRepository<MenuTag, MenuTagId> {
    void deleteByMenuId(Long menuId);
    void deleteByMenuIdIn(Collection<Long> menuIds);
    List<MenuTag> findByMenuId(Long menuId);
    List<MenuTag> findByMenuIdIn(Collection<Long> menuIds);
}
