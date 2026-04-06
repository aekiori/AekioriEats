package com.delivery.user.dto.request;

import com.delivery.user.domain.user.User;
import jakarta.validation.constraints.NotNull;

public record UpdateUserStatusDto(
    @NotNull User.Status status
) {
}
