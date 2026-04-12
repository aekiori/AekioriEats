package com.delivery.store.service.store;

import com.delivery.store.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class StoreAuthorizationService {
    private static final String ADMIN_ROLE = "ADMIN";

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

    public void requireSelfOrAdmin(long authenticatedUserId, long targetUserId, String authenticatedUserRole) {
        if (isAdmin(authenticatedUserRole)) {
            return;
        }

        if (authenticatedUserId != targetUserId) {
            throw forbiddenResourceAccess();
        }
    }

    public void requireStoreOwnerOrAdmin(long authenticatedUserId, long ownerUserId, String authenticatedUserRole) {
        requireSelfOrAdmin(authenticatedUserId, ownerUserId, authenticatedUserRole);
    }

    private boolean isAdmin(String role) {
        return role != null && ADMIN_ROLE.equalsIgnoreCase(role.trim());
    }

    private ApiException unauthorizedPrincipal() {
        return new ApiException(
            "UNAUTHORIZED_PRINCIPAL",
            "Authenticated principal is missing or invalid.",
            HttpStatus.UNAUTHORIZED
        );
    }

    private ApiException forbiddenResourceAccess() {
        return new ApiException(
            "FORBIDDEN_RESOURCE_ACCESS",
            "You do not have permission to access this resource.",
            HttpStatus.FORBIDDEN
        );
    }
}
