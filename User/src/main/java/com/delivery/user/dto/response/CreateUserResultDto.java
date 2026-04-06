package com.delivery.user.dto.response;

import com.delivery.user.domain.user.User;

import java.time.LocalDateTime;

public record CreateUserResultDto(
    Long userId,
    String email,
    String status,
    LocalDateTime createdAt
) {
    public static CreateUserResultDto from(User user) {
        return new CreateUserResultDto(
            user.getId(),
            user.getEmail(),
            user.getStatus().name(),
            user.getCreatedAt()
        );
    }
}
