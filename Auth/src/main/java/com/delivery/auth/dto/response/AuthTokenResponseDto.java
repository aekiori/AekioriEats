package com.delivery.auth.dto.response;

public record AuthTokenResponseDto(
    String accessToken,
    String refreshToken,
    String tokenType,
    long expiresIn
) {
}
