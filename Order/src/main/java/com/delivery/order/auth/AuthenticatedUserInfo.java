package com.delivery.order.auth;

public record AuthenticatedUserInfo(
    long userId,
    String userRole
) {
}
