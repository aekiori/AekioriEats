package com.delivery.store.service.store;

import com.delivery.store.domain.category.Category;
import com.delivery.store.domain.menu.Menu;
import com.delivery.store.domain.option.MenuOptionGroup;
import com.delivery.store.domain.store.Store;
import com.delivery.store.domain.store.StoreCategory;
import com.delivery.store.dto.request.UpdateStoreStatusDto;
import com.delivery.store.dto.request.owner.CreateOwnerStoreRequest;
import com.delivery.store.dto.request.owner.DeliveryPolicyRequest;
import com.delivery.store.dto.request.owner.UpdateOwnerStoreRequest;
import com.delivery.store.dto.response.CreateStoreResultDto;
import com.delivery.store.dto.response.OwnerStoreSummaryResultDto;
import com.delivery.store.dto.response.StoreDetailResultDto;
import com.delivery.store.exception.ApiException;
import com.delivery.store.repository.category.CategoryRepository;
import com.delivery.store.repository.menu.MenuGroupRepository;
import com.delivery.store.repository.menu.MenuRepository;
import com.delivery.store.repository.menu.MenuTagRepository;
import com.delivery.store.repository.option.MenuOptionGroupRepository;
import com.delivery.store.repository.option.MenuOptionRepository;
import com.delivery.store.repository.store.StoreCategoryRepository;
import com.delivery.store.repository.store.StoreHolidayRepository;
import com.delivery.store.repository.store.StoreHourRepository;
import com.delivery.store.repository.store.StoreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StoreService {
    private final StoreRepository storeRepository;
    private final StoreAuthorizationService storeAuthorizationService;
    private final StoreDomainSupport storeDomainSupport;
    private final CategoryRepository categoryRepository;
    private final StoreCategoryRepository storeCategoryRepository;
    private final StoreHourRepository storeHourRepository;
    private final StoreHolidayRepository storeHolidayRepository;
    private final MenuRepository menuRepository;
    private final MenuGroupRepository menuGroupRepository;
    private final MenuTagRepository menuTagRepository;
    private final MenuOptionGroupRepository menuOptionGroupRepository;
    private final MenuOptionRepository menuOptionRepository;

    @Transactional
    public CreateStoreResultDto createOwnerStore(
        CreateOwnerStoreRequest request,
        long authenticatedUserId
    ) {
        Store savedStore;
        try {
            savedStore = storeRepository.save(Store.create(authenticatedUserId, request.name().trim()));
        } catch (DataIntegrityViolationException exception) {
            throw storeNameConflict();
        }

        if (request.deliveryPolicy() != null) {
            savedStore.updateDeliveryPolicy(
                request.deliveryPolicy().minOrderAmount(),
                request.deliveryPolicy().deliveryTip()
            );
        }

        if (request.storeLogoUrl() != null && !request.storeLogoUrl().isBlank()) {
            savedStore.updateStoreLogoUrl(request.storeLogoUrl().trim());
        }

        savedStore = storeRepository.save(savedStore);
        replaceStoreCategories(savedStore.getId(), request.categoryIds());
        return CreateStoreResultDto.from(savedStore);
    }

    @Transactional(readOnly = true)
    public StoreDetailResultDto getStore(Long storeId, long authenticatedUserId) {
        Store store = storeDomainSupport.findStore(storeId);
        storeAuthorizationService.requireStoreOwner(authenticatedUserId, store.getOwnerUserId());
        return StoreDetailResultDto.from(store);
    }

    @Transactional(readOnly = true)
    public List<OwnerStoreSummaryResultDto> getOwnerStores(long authenticatedUserId) {
        return storeRepository.findByOwnerUserIdOrderByCreatedAtDesc(authenticatedUserId)
            .stream()
            .map(OwnerStoreSummaryResultDto::from)
            .toList();
    }

    @Transactional
    public StoreDetailResultDto updateStoreStatus(
        Long storeId,
        UpdateStoreStatusDto request,
        long authenticatedUserId
    ) {
        Store store = storeDomainSupport.requireOwnedStore(storeId, authenticatedUserId);
        store.updateStatus(request.status());
        return StoreDetailResultDto.from(storeRepository.save(store));
    }

    @Transactional
    public StoreDetailResultDto replaceDeliveryPolicy(
        Long storeId,
        DeliveryPolicyRequest request,
        long authenticatedUserId
    ) {
        Store store = storeDomainSupport.requireOwnedStore(storeId, authenticatedUserId);
        store.updateDeliveryPolicy(request.minOrderAmount(), request.deliveryTip());
        return StoreDetailResultDto.from(storeRepository.save(store));
    }

    @Transactional
    public StoreDetailResultDto updateOwnerStore(
        Long storeId,
        UpdateOwnerStoreRequest request,
        long authenticatedUserId
    ) {
        Store store = storeDomainSupport.requireOwnedStore(storeId, authenticatedUserId);
        store.updateName(request.name().trim());

        if (request.deliveryPolicy() != null) {
            store.updateDeliveryPolicy(
                request.deliveryPolicy().minOrderAmount(),
                request.deliveryPolicy().deliveryTip()
            );
        }
        store.updateStoreLogoUrl(normalizeOptionalString(request.storeLogoUrl()));

        Store savedStore;
        try {
            savedStore = storeRepository.save(store);
        } catch (DataIntegrityViolationException exception) {
            throw storeNameConflict();
        }

        if (request.categoryIds() != null) {
            replaceStoreCategories(savedStore.getId(), request.categoryIds());
        }

        return StoreDetailResultDto.from(savedStore);
    }

    @Transactional
    public void deleteOwnerStore(Long storeId, long authenticatedUserId) {
        Store store = storeDomainSupport.requireOwnedStore(storeId, authenticatedUserId);
        deleteStoreChildren(store.getId());
        storeRepository.delete(store);
    }

    private void replaceStoreCategories(Long storeId, List<Long> categoryIds) {
        storeCategoryRepository.deleteByStoreId(storeId);
        if (categoryIds == null || categoryIds.isEmpty()) {
            return;
        }

        List<Long> normalizedIds = categoryIds.stream()
            .filter(id -> id != null && id > 0)
            .distinct()
            .toList();
        if (normalizedIds.isEmpty()) {
            return;
        }

        List<Category> categories = categoryRepository.findAllById(normalizedIds);
        if (categories.size() != normalizedIds.size()) {
            throw new ApiException(
                "INVALID_CATEGORY_IDS",
                "One or more categories were not found.",
                HttpStatus.BAD_REQUEST
            );
        }

        List<StoreCategory> storeCategories = normalizedIds.stream()
            .map(categoryId -> new StoreCategory(storeId, categoryId))
            .toList();
        storeCategoryRepository.saveAll(storeCategories);
    }

    private void deleteStoreChildren(Long storeId) {
        List<Menu> menus = menuRepository.findByStoreIdOrderByDisplayOrderAscIdAsc(storeId);
        if (!menus.isEmpty()) {
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

        menuGroupRepository.deleteByStoreId(storeId);
        storeHolidayRepository.deleteByStoreId(storeId);
        storeHourRepository.deleteByStoreId(storeId);
        storeCategoryRepository.deleteByStoreId(storeId);
    }

    private String normalizeOptionalString(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private ApiException storeNameConflict() {
        return new ApiException(
            "STORE_NAME_ALREADY_EXISTS_FOR_OWNER",
            "Store name is already in use for this owner.",
            HttpStatus.CONFLICT
        );
    }
}
