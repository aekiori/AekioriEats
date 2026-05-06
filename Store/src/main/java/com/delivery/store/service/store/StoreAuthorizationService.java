package com.delivery.store.service.store;

import com.delivery.store.exception.ApiException;
import com.delivery.store.exception.StoreErrorCode;
import org.springframework.stereotype.Component;

@Component
public class StoreAuthorizationService {
    public long parseAuthenticatedUserId(String rawUserIdHeader) {
        if (rawUserIdHeader == null || rawUserIdHeader.isBlank()) {
            throw unauthorizedPrincipal();
        }

        try {
            long authenticatedUserId = Long.parseLong(rawUserIdHeader.trim());
            if (authenticatedUserId <= 0) {
                throw unauthorizedPrincipal();
            }
            return authenticatedUserId;
        } catch (NumberFormatException exception) {
            throw unauthorizedPrincipal();
        }
    }

    public void requireSelf(long authenticatedUserId, long targetUserId) {
        if (authenticatedUserId != targetUserId) {
            throw forbiddenResourceAccess();
        }
    }

    public void requireStoreOwner(long authenticatedUserId, long ownerUserId) {
        requireSelf(authenticatedUserId, ownerUserId);
    }

    private ApiException unauthorizedPrincipal() {
        return new ApiException(StoreErrorCode.UNAUTHORIZED_PRINCIPAL);
    }

    private ApiException forbiddenResourceAccess() {
        return new ApiException(StoreErrorCode.FORBIDDEN_RESOURCE_ACCESS);
    }
}
