package com.delivery.store.repository.option;

import com.delivery.store.domain.option.MenuOption;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface MenuOptionRepository extends JpaRepository<MenuOption, Long> {
    void deleteByOptionGroupIdIn(Collection<Long> optionGroupIds);
    List<MenuOption> findByOptionGroupIdInOrderByDisplayOrderAscIdAsc(Collection<Long> optionGroupIds);
}
