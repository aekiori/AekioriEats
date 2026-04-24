package com.delivery.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SignupRequestDto(
    @Schema(description = "회원가입 이메일", example = "order-tester@example.com")
    @NotBlank
    @Email
    @Size(max = 100)
    String email,

    @Schema(description = "회원가입 비밀번호", example = "Passw0rd!")
    @NotBlank
    @Size(min = 8, max = 72)
    String password
) {
}
