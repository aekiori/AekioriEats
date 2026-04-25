package com.delivery.user.auth;

public record AuthenticatedUserInfo(
    long userId,
    String userRole
) {
}
