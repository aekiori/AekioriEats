package com.delivery.order.service.order;

import com.delivery.order.exception.ApiException;
import com.delivery.order.exception.OrderErrorCode;
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
        return new ApiException(OrderErrorCode.UNAUTHORIZED_PRINCIPAL);
    }

    private ApiException forbiddenResourceAccess() {
        return new ApiException(OrderErrorCode.FORBIDDEN_RESOURCE_ACCESS);
    }
}
