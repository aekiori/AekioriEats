package com.delivery.point.service.point;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class PointAuthorizationService {
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
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "You do not have permission to access this resource."
            );
        }
    }

    private ResponseStatusException unauthorizedPrincipal() {
        return new ResponseStatusException(
            HttpStatus.UNAUTHORIZED,
            "Authenticated principal is missing or invalid."
        );
    }
}
