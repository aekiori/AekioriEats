package com.delivery.user.domain.user.event;

import com.delivery.user.domain.user.User;

public record UserStatusChangedOutboxEvent(
    Long userId,
    String email,
    User.Status currentStatus,
    User.Status targetStatus,
    String reason
) {
    public static UserStatusChangedOutboxEvent from(
        User user,
        User.Status currentStatus,
        User.Status targetStatus,
        String reason
    ) {
        return new UserStatusChangedOutboxEvent(
            user.getId(),
            user.getEmail(),
            currentStatus,
            targetStatus,
            reason
        );
    }
}
