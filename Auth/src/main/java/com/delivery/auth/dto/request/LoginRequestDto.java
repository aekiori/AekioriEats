package com.delivery.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequestDto(
    @Schema(description = "로그인 이메일", example = "order-tester@example.com")
    @NotBlank
    @Email
    String email,

    @Schema(description = "로그인 비밀번호", example = "Passw0rd!")
    @NotBlank
    String password
) {
}
