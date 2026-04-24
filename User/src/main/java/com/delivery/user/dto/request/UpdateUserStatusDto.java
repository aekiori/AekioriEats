package com.delivery.user.dto.request;

import com.delivery.user.domain.user.User;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record UpdateUserStatusDto(
    @Schema(description = "변경할 사용자 상태", example = "LOCKED")
    @NotNull
    User.Status status
) {
}
