package com.delivery.store.auth;

public record AuthenticatedUserInfo(
    long userId,
    String userRole
) {
}
