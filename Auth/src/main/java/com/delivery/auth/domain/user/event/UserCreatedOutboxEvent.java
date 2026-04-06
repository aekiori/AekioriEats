package com.delivery.auth.domain.user.event;

import com.delivery.auth.domain.user.AuthUser;

public record UserCreatedOutboxEvent(
    Long userId,
    String email,
    String status
) {
    public static UserCreatedOutboxEvent from(AuthUser authUser) {
        return new UserCreatedOutboxEvent(
            authUser.getUserId(),
            authUser.getEmail(),
            authUser.getStatus()
        );
    }
}
