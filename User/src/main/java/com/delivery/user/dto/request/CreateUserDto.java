package com.delivery.user.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CreateUserDto(
    @NotBlank @Email String email
) {
}
