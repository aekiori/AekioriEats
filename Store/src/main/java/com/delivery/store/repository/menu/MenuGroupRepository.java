package com.delivery.store.repository.menu;

import com.delivery.store.domain.menu.MenuGroup;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MenuGroupRepository extends JpaRepository<MenuGroup, Long> {
    void deleteByStoreId(Long storeId);
    List<MenuGroup> findByStoreIdOrderByDisplayOrderAscIdAsc(Long storeId);
    Optional<MenuGroup> findByIdAndStoreId(Long id, Long storeId);
}
