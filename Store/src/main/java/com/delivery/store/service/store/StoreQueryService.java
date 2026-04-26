package com.delivery.store.service.store;

import com.delivery.store.domain.menu.Menu;
import com.delivery.store.domain.menu.MenuGroup;
import com.delivery.store.domain.menu.MenuTag;
import com.delivery.store.domain.menu.Tag;
import com.delivery.store.domain.option.MenuOption;
import com.delivery.store.domain.option.MenuOptionGroup;
import com.delivery.store.domain.store.Store;
import com.delivery.store.domain.store.StoreHoliday;
import com.delivery.store.domain.store.StoreHour;
import com.delivery.store.dto.request.StoreSearchRequestDto;
import com.delivery.store.dto.response.query.StoreQueryResponseDto;
import com.delivery.store.repository.category.CategoryRepository;
import com.delivery.store.repository.menu.MenuGroupRepository;
import com.delivery.store.repository.menu.MenuRepository;
import com.delivery.store.repository.menu.MenuTagRepository;
import com.delivery.store.repository.menu.TagRepository;
import com.delivery.store.repository.option.MenuOptionGroupRepository;
import com.delivery.store.repository.option.MenuOptionRepository;
import com.delivery.store.repository.store.StoreCategoryRepository;
import com.delivery.store.repository.store.StoreHolidayRepository;
import com.delivery.store.repository.store.StoreHourRepository;
import com.delivery.store.repository.store.StoreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StoreQueryService {
    private static final long UNGROUPED_MENU_GROUP_ID = -1L;
    private static final String UNGROUPED_MENU_GROUP_NAME = "UNGROUPED";

    private final StoreRepository storeRepository;
    private final StoreDomainSupport storeDomainSupport;
    private final CategoryRepository categoryRepository;
    private final MenuGroupRepository menuGroupRepository;
    private final MenuRepository menuRepository;
    private final TagRepository tagRepository;
    private final MenuTagRepository menuTagRepository;
    private final StoreHourRepository storeHourRepository;
    private final StoreHolidayRepository storeHolidayRepository;
    private final MenuOptionGroupRepository menuOptionGroupRepository;
    private final MenuOptionRepository menuOptionRepository;
    private final StoreCategoryRepository storeCategoryRepository;

    @Transactional(readOnly = true)
    public List<StoreQueryResponseDto.CategoryResponseDto> getCategories() {
        return categoryRepository.findAll(Sort.by(Sort.Direction.ASC, "id")).stream()
            .map(category -> new StoreQueryResponseDto.CategoryResponseDto(category.getId(), category.getName()))
            .toList();
    }

    @Transactional(readOnly = true)
    public StoreQueryResponseDto.StoreSearchPageResponseDto searchStores(StoreSearchRequestDto request) {
        Page<Store> stores = storeRepository.findByNameContainingIgnoreCase(
            request.resolvedQuery(),
            PageRequest.of(request.resolvedPage(), request.resolvedSize())
        );
        List<StoreQueryResponseDto.StoreSearchItemResponseDto> content = stores.getContent().stream()
            .map(store -> new StoreQueryResponseDto.StoreSearchItemResponseDto(
                store.getId(),
                store.getName(),
                store.getStatus().name(),
                new StoreQueryResponseDto.DeliveryPolicyResponseDto(store.getMinOrderAmount(), store.getDeliveryTip()),
                store.getStoreLogoUrl(),
                List.of("STORE_NAME")
            ))
            .toList();

        return new StoreQueryResponseDto.StoreSearchPageResponseDto(
            content,
            stores.getNumber(),
            stores.getSize(),
            stores.getTotalElements(),
            stores.getTotalPages()
        );
    }

    @Transactional(readOnly = true)
    public StoreQueryResponseDto.StoreDetailResponseDto getStoreDetail(Long storeId) {
        Store store = storeDomainSupport.findStore(storeId);
        return toStoreDetailDto(store);
    }

    private StoreQueryResponseDto.StoreDetailResponseDto toStoreDetailDto(Store store) {
        Long storeId = store.getId();

        List<MenuGroup> menuGroups = menuGroupRepository.findByStoreIdOrderByDisplayOrderAscIdAsc(storeId);
        List<Menu> menus = menuRepository.findByStoreIdOrderByDisplayOrderAscIdAsc(storeId);
        Map<Long, List<StoreQueryResponseDto.MenuResponseDto>> menusByGroupId = buildMenusByGroupId(menus);
        List<StoreQueryResponseDto.MenuGroupResponseDto> menuGroupDtos = buildMenuGroupDtos(menuGroups, menusByGroupId);

        return new StoreQueryResponseDto.StoreDetailResponseDto(
            store.getId(),
            store.getOwnerUserId(),
            store.getName(),
            store.getStatus().name(),
            new StoreQueryResponseDto.DeliveryPolicyResponseDto(store.getMinOrderAmount(), store.getDeliveryTip()),
            new StoreQueryResponseDto.ImagesResponseDto(store.getStoreLogoUrl()),
            loadCategories(storeId),
            loadOperatingHours(storeId),
            loadHolidays(storeId),
            menuGroupDtos
        );
    }

    private List<StoreQueryResponseDto.CategoryResponseDto> loadCategories(Long storeId) {
        return storeCategoryRepository.findCategoriesByStoreId(storeId);
    }

    private List<StoreQueryResponseDto.StoreHourResponseDto> loadOperatingHours(Long storeId) {
        return storeHourRepository.findHoursByStoreId(storeId);
    }

    private List<StoreQueryResponseDto.StoreHolidayResponseDto> loadHolidays(Long storeId) {
        return storeHolidayRepository.findUpcomingHolidaysByStoreId(storeId, LocalDate.now());
    }

    private Map<Long, List<StoreQueryResponseDto.MenuResponseDto>> buildMenusByGroupId(List<Menu> menus) {
        if (menus.isEmpty()) {
            return Map.of();
        }

        List<Long> menuIds = menus.stream().map(Menu::getId).toList();
        Map<Long, List<StoreQueryResponseDto.TagResponseDto>> tagsByMenuId = loadTagsByMenuId(menuIds);
        Map<Long, List<StoreQueryResponseDto.OptionGroupResponseDto>> optionGroupsByMenuId = loadOptionGroupsByMenuId(menuIds);

        Map<Long, List<StoreQueryResponseDto.MenuResponseDto>> menusByGroupId = new HashMap<>();
        for (Menu menu : menus) {
            Long menuGroupId = menu.getMenuGroup() != null ? menu.getMenuGroup().getId() : UNGROUPED_MENU_GROUP_ID;
            StoreQueryResponseDto.MenuResponseDto menuDto = new StoreQueryResponseDto.MenuResponseDto(
                menu.getId(),
                menu.getName(),
                menu.getDescription(),
                menu.getPrice(),
                menu.isAvailable(),
                menu.isSoldOut(),
                menu.getMenuImageUrl(),
                tagsByMenuId.getOrDefault(menu.getId(), List.of()),
                optionGroupsByMenuId.getOrDefault(menu.getId(), List.of())
            );
            menusByGroupId.computeIfAbsent(menuGroupId, key -> new ArrayList<>()).add(menuDto);
        }

        return menusByGroupId;
    }

    private List<StoreQueryResponseDto.MenuGroupResponseDto> buildMenuGroupDtos(
        List<MenuGroup> menuGroups,
        Map<Long, List<StoreQueryResponseDto.MenuResponseDto>> menusByGroupId
    ) {
        List<StoreQueryResponseDto.MenuGroupResponseDto> menuGroupDtos = new ArrayList<>();
        for (MenuGroup menuGroup : menuGroups) {
            menuGroupDtos.add(new StoreQueryResponseDto.MenuGroupResponseDto(
                menuGroup.getId(),
                menuGroup.getName(),
                menusByGroupId.getOrDefault(menuGroup.getId(), List.of())
            ));
        }

        List<StoreQueryResponseDto.MenuResponseDto> ungroupedMenus = menusByGroupId.getOrDefault(UNGROUPED_MENU_GROUP_ID, List.of());
        if (!ungroupedMenus.isEmpty()) {
            menuGroupDtos.add(new StoreQueryResponseDto.MenuGroupResponseDto(
                UNGROUPED_MENU_GROUP_ID,
                UNGROUPED_MENU_GROUP_NAME,
                ungroupedMenus
            ));
        }
        return menuGroupDtos;
    }

    private Map<Long, List<StoreQueryResponseDto.TagResponseDto>> loadTagsByMenuId(Collection<Long> menuIds) {
        if (menuIds.isEmpty()) {
            return Map.of();
        }
        List<MenuTag> menuTags = menuTagRepository.findByMenuIdIn(menuIds);
        Set<Long> tagIds = menuTags.stream().map(MenuTag::getTagId).collect(Collectors.toSet());
        Map<Long, Tag> tagsById = tagRepository.findAllById(tagIds).stream()
            .collect(Collectors.toMap(Tag::getId, tag -> tag));

        Map<Long, List<StoreQueryResponseDto.TagResponseDto>> tagsByMenuId = new HashMap<>();
        for (MenuTag menuTag : menuTags) {
            Tag tag = tagsById.get(menuTag.getTagId());
            if (tag == null) {
                continue;
            }
            tagsByMenuId.computeIfAbsent(menuTag.getMenuId(), key -> new ArrayList<>())
                .add(new StoreQueryResponseDto.TagResponseDto(tag.getId(), tag.getName()));
        }
        return tagsByMenuId;
    }

    private Map<Long, List<StoreQueryResponseDto.OptionGroupResponseDto>> loadOptionGroupsByMenuId(Collection<Long> menuIds) {
        if (menuIds.isEmpty()) {
            return Map.of();
        }
        List<MenuOptionGroup> optionGroups = menuOptionGroupRepository.findByMenuIdInOrderByDisplayOrderAscIdAsc(menuIds);
        if (optionGroups.isEmpty()) {
            return Map.of();
        }

        List<Long> optionGroupIds = optionGroups.stream().map(MenuOptionGroup::getId).toList();
        List<MenuOption> options = menuOptionRepository.findByOptionGroupIdInOrderByDisplayOrderAscIdAsc(optionGroupIds);
        Map<Long, List<StoreQueryResponseDto.OptionResponseDto>> optionsByGroupId = new HashMap<>();
        for (MenuOption option : options) {
            optionsByGroupId.computeIfAbsent(option.getOptionGroup().getId(), key -> new ArrayList<>())
                .add(new StoreQueryResponseDto.OptionResponseDto(
                    option.getName(),
                    option.getExtraPrice(),
                    option.isAvailable()
                ));
        }

        Map<Long, List<StoreQueryResponseDto.OptionGroupResponseDto>> groupsByMenuId = new HashMap<>();
        for (MenuOptionGroup optionGroup : optionGroups) {
            groupsByMenuId.computeIfAbsent(optionGroup.getMenu().getId(), key -> new ArrayList<>())
                .add(new StoreQueryResponseDto.OptionGroupResponseDto(
                    optionGroup.getName(),
                    optionGroup.isRequired(),
                    optionGroup.isMultiple(),
                    optionGroup.getMinSelectCount(),
                    optionGroup.getMaxSelectCount(),
                    optionsByGroupId.getOrDefault(optionGroup.getId(), List.of())
                ));
        }

        return groupsByMenuId;
    }
}
