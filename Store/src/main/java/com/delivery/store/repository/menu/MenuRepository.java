package com.delivery.store.repository.menu;

import com.delivery.store.domain.menu.Menu;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MenuRepository extends JpaRepository<Menu, Long> {
    List<Menu> findByStoreIdOrderByDisplayOrderAscIdAsc(Long storeId);
    List<Menu> findByStoreIdAndMenuGroupIdOrderByDisplayOrderAscIdAsc(Long storeId, Long menuGroupId);
    Optional<Menu> findByIdAndStoreId(Long id, Long storeId);
    List<Menu> findByMenuGroupIdInOrderByDisplayOrderAscIdAsc(List<Long> menuGroupIds);
}
