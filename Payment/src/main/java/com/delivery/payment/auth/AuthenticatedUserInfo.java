package com.delivery.payment.auth;

public record AuthenticatedUserInfo(
    long userId,
    String userRole
) {
}
