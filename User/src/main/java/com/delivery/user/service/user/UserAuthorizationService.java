package com.delivery.user.service.user;

import com.delivery.user.exception.ApiException;
import com.delivery.user.exception.UserErrorCode;
import org.springframework.stereotype.Component;

@Component
public class UserAuthorizationService {
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
        return new ApiException(UserErrorCode.UNAUTHORIZED_PRINCIPAL);
    }

    private ApiException forbiddenResourceAccess() {
        return new ApiException(UserErrorCode.FORBIDDEN_RESOURCE_ACCESS);
    }
}
