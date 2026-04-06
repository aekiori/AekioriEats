package com.delivery.user.domain.user.exception;

import com.delivery.user.domain.user.User;

public class InvalidUserStatusTransitionException extends RuntimeException {
    public InvalidUserStatusTransitionException(User.Status currentStatus, User.Status targetStatus) {
        super("Invalid user status transition. currentStatus=%s, targetStatus=%s".formatted(currentStatus, targetStatus));
    }
}
