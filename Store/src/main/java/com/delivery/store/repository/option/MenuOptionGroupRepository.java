package com.delivery.store.repository.option;

import com.delivery.store.domain.option.MenuOptionGroup;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface MenuOptionGroupRepository extends JpaRepository<MenuOptionGroup, Long> {
    void deleteByMenuId(Long menuId);
    void deleteByMenuIdIn(Collection<Long> menuIds);
    List<MenuOptionGroup> findByMenuIdOrderByDisplayOrderAscIdAsc(Long menuId);
    List<MenuOptionGroup> findByMenuIdInOrderByDisplayOrderAscIdAsc(Collection<Long> menuIds);
}
