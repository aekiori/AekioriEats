package com.delivery.store.service.store;

import com.delivery.store.domain.menu.Menu;
import com.delivery.store.domain.menu.MenuGroup;
import com.delivery.store.domain.store.Store;
import com.delivery.store.exception.ApiException;
import com.delivery.store.exception.StoreErrorCode;
import com.delivery.store.repository.menu.MenuGroupRepository;
import com.delivery.store.repository.menu.MenuRepository;
import com.delivery.store.repository.store.StoreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StoreDomainSupport {
    private final StoreRepository storeRepository;
    private final MenuRepository menuRepository;
    private final MenuGroupRepository menuGroupRepository;
    private final StoreAuthorizationService storeAuthorizationService;

    public Store requireOwnedStore(Long storeId, long authenticatedUserId) {
        Store store = findStore(storeId);
        storeAuthorizationService.requireStoreOwner(authenticatedUserId, store.getOwnerUserId());
        return store;
    }

    public Store requireOwnedStore(Long storeId, long authenticatedUserId, String authenticatedUserRole) {
        return requireOwnedStore(storeId, authenticatedUserId);
    }

    public Store findStore(Long storeId) {
        return storeRepository.findById(storeId)
            .orElseThrow(() -> new ApiException(StoreErrorCode.STORE_NOT_FOUND));
    }

    public Menu findMenu(Long menuId, Long storeId) {
        return menuRepository.findByIdAndStoreId(menuId, storeId)
            .orElseThrow(() -> new ApiException(StoreErrorCode.MENU_NOT_FOUND));
    }

    public MenuGroup resolveMenuGroup(Long menuGroupId, Long storeId) {
        if (menuGroupId == null) {
            return null;
        }
        return menuGroupRepository.findByIdAndStoreId(menuGroupId, storeId)
            .orElseThrow(() -> new ApiException(StoreErrorCode.MENU_GROUP_NOT_FOUND_FOR_STORE));
    }
}
