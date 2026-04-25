package com.delivery.point.auth;

public record AuthenticatedUserInfo(
    long userId,
    String userRole
) {
}
