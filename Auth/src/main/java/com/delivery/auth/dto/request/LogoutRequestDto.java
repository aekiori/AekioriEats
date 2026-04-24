package com.delivery.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record LogoutRequestDto(
    @Schema(description = "로그아웃할 refresh token", example = "eyJhbGciOiJIUzI1NiJ9.refresh.token")
    @NotBlank
    String refreshToken
) {
}
