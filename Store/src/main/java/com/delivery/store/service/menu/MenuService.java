package com.delivery.store.service.menu;

import com.delivery.store.domain.menu.Menu;
import com.delivery.store.domain.menu.MenuGroup;
import com.delivery.store.domain.menu.MenuTag;
import com.delivery.store.domain.menu.Tag;
import com.delivery.store.domain.option.MenuOptionGroup;
import com.delivery.store.domain.store.Store;
import com.delivery.store.dto.request.owner.CreateMenuGroupRequest;
import com.delivery.store.dto.request.owner.CreateMenuRequest;
import com.delivery.store.dto.request.owner.ReplaceMenuTagsRequest;
import com.delivery.store.dto.request.owner.UpdateMenuGroupRequest;
import com.delivery.store.dto.request.owner.UpdateMenuRequest;
import com.delivery.store.dto.response.MenuGroupResultDto;
import com.delivery.store.dto.response.MenuResultDto;
import com.delivery.store.dto.response.ReplaceMenuTagsResultDto;
import com.delivery.store.exception.ApiException;
import com.delivery.store.repository.menu.MenuGroupRepository;
import com.delivery.store.repository.menu.MenuRepository;
import com.delivery.store.repository.menu.MenuTagRepository;
import com.delivery.store.repository.menu.TagRepository;
import com.delivery.store.repository.option.MenuOptionGroupRepository;
import com.delivery.store.repository.option.MenuOptionRepository;
import com.delivery.store.service.store.StoreDomainSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MenuService {
    private final StoreDomainSupport storeDomainSupport;
    private final MenuGroupRepository menuGroupRepository;
    private final MenuRepository menuRepository;
    private final TagRepository tagRepository;
    private final MenuTagRepository menuTagRepository;
    private final MenuOptionGroupRepository menuOptionGroupRepository;
    private final MenuOptionRepository menuOptionRepository;

    @Transactional
    public MenuGroupResultDto createMenuGroup(
        Long storeId,
        CreateMenuGroupRequest request,
        long authenticatedUserId,
        String authenticatedUserRole
    ) {
        Store store = storeDomainSupport.requireOwnedStore(storeId, authenticatedUserId, authenticatedUserRole);
        MenuGroup saved = menuGroupRepository.save(
            MenuGroup.create(store, request.name().trim(), request.displayOrder())
        );
        return MenuGroupResultDto.from(saved);
    }

    @Transactional
    public MenuResultDto createMenu(
        Long storeId,
        CreateMenuRequest request,
        long authenticatedUserId,
        String authenticatedUserRole
    ) {
        Store store = storeDomainSupport.requireOwnedStore(storeId, authenticatedUserId, authenticatedUserRole);
        MenuGroup menuGroup = storeDomainSupport.resolveMenuGroup(request.menuGroupId(), storeId);

        Menu savedMenu = menuRepository.save(Menu.create(
            store,
            menuGroup,
            request.name().trim(),
            nullableTrim(request.description()),
            request.price(),
            request.isAvailableOrDefault(),
            false,
            nullableTrim(request.imageUrl()),
            request.displayOrderOrDefault()
        ));

        return MenuResultDto.from(savedMenu);
    }

    @Transactional
    public MenuResultDto updateMenu(
        Long storeId,
        Long menuId,
        UpdateMenuRequest request,
        long authenticatedUserId,
        String authenticatedUserRole
    ) {
        storeDomainSupport.requireOwnedStore(storeId, authenticatedUserId, authenticatedUserRole);
        Menu menu = storeDomainSupport.findMenu(menuId, storeId);
        MenuGroup menuGroup = storeDomainSupport.resolveMenuGroup(request.menuGroupId(), storeId);
        menu.update(
            menuGroup,
            request.name().trim(),
            nullableTrim(request.description()),
            request.price(),
            request.isAvailable(),
            request.isSoldOut(),
            nullableTrim(request.imageUrl())
        );
        return MenuResultDto.from(menuRepository.save(menu));
    }

    @Transactional
    public MenuGroupResultDto updateMenuGroup(
        Long storeId,
        Long menuGroupId,
        UpdateMenuGroupRequest request,
        long authenticatedUserId,
        String authenticatedUserRole
    ) {
        storeDomainSupport.requireOwnedStore(storeId, authenticatedUserId, authenticatedUserRole);
        MenuGroup menuGroup = menuGroupRepository.findByIdAndStoreId(menuGroupId, storeId)
            .orElseThrow(() -> new ApiException(
                "MENU_GROUP_NOT_FOUND",
                "Menu group was not found.",
                HttpStatus.NOT_FOUND
            ));
        menuGroup.update(request.name().trim(), request.displayOrder());
        return MenuGroupResultDto.from(menuGroupRepository.save(menuGroup));
    }

    @Transactional
    public ReplaceMenuTagsResultDto replaceMenuTags(
        Long storeId,
        Long menuId,
        ReplaceMenuTagsRequest request,
        long authenticatedUserId,
        String authenticatedUserRole
    ) {
        storeDomainSupport.requireOwnedStore(storeId, authenticatedUserId, authenticatedUserRole);
        storeDomainSupport.findMenu(menuId, storeId);

        Set<String> normalizedNames = request.tagNames().stream()
            .filter(name -> name != null && !name.isBlank())
            .map(String::trim)
            .collect(Collectors.toCollection(LinkedHashSet::new));

        List<Tag> existingTags = tagRepository.findByNameIn(normalizedNames);
        Map<String, Tag> byName = existingTags.stream()
            .collect(Collectors.toMap(Tag::getName, tag -> tag, (left, right) -> left, LinkedHashMap::new));

        List<Tag> newTags = normalizedNames.stream()
            .filter(name -> !byName.containsKey(name))
            .map(Tag::create)
            .toList();
        List<Tag> savedNewTags = tagRepository.saveAll(newTags);
        savedNewTags.forEach(tag -> byName.put(tag.getName(), tag));

        menuTagRepository.deleteByMenuId(menuId);
        List<MenuTag> menuTags = byName.values().stream()
            .map(tag -> new MenuTag(menuId, tag.getId()))
            .toList();
        menuTagRepository.saveAll(menuTags);

        List<ReplaceMenuTagsResultDto.TagRefDto> tagResults = byName.values().stream()
            .map(tag -> new ReplaceMenuTagsResultDto.TagRefDto(tag.getId(), tag.getName()))
            .toList();

        return new ReplaceMenuTagsResultDto(menuId, tagResults);
    }

    @Transactional
    public void deleteMenu(
        Long storeId,
        Long menuId,
        long authenticatedUserId,
        String authenticatedUserRole
    ) {
        storeDomainSupport.requireOwnedStore(storeId, authenticatedUserId, authenticatedUserRole);
        Menu menu = storeDomainSupport.findMenu(menuId, storeId);
        deleteMenus(List.of(menu));
    }

    @Transactional
    public void deleteMenuGroup(
        Long storeId,
        Long menuGroupId,
        long authenticatedUserId,
        String authenticatedUserRole
    ) {
        storeDomainSupport.requireOwnedStore(storeId, authenticatedUserId, authenticatedUserRole);
        MenuGroup menuGroup = menuGroupRepository.findByIdAndStoreId(menuGroupId, storeId)
            .orElseThrow(() -> new ApiException(
                "MENU_GROUP_NOT_FOUND",
                "Menu group was not found.",
                HttpStatus.NOT_FOUND
            ));

        List<Menu> menus = menuRepository.findByStoreIdAndMenuGroupIdOrderByDisplayOrderAscIdAsc(storeId, menuGroupId);
        deleteMenus(menus);
        menuGroupRepository.delete(menuGroup);
    }

    private void deleteMenus(List<Menu> menus) {
        if (menus.isEmpty()) {
            return;
        }

        List<Long> menuIds = menus.stream().map(Menu::getId).toList();
        List<MenuOptionGroup> optionGroups = menuOptionGroupRepository.findByMenuIdInOrderByDisplayOrderAscIdAsc(menuIds);
        if (!optionGroups.isEmpty()) {
            List<Long> optionGroupIds = optionGroups.stream().map(MenuOptionGroup::getId).toList();
            menuOptionRepository.deleteByOptionGroupIdIn(optionGroupIds);
        }

        menuOptionGroupRepository.deleteByMenuIdIn(menuIds);
        menuTagRepository.deleteByMenuIdIn(menuIds);
        menuRepository.deleteAll(menus);
    }

    private String nullableTrim(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
