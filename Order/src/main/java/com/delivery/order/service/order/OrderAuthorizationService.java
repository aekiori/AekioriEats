package com.delivery.order.service.order;

import com.delivery.order.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class OrderAuthorizationService {
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
