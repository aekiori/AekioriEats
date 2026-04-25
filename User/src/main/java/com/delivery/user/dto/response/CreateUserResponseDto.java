package com.delivery.user.dto.response;

import com.delivery.user.domain.user.User;

import java.time.LocalDateTime;

public record CreateUserResponseDto(
    Long userId,
    String email,
    String status,
    LocalDateTime createdAt
) {
    public static CreateUserResponseDto from(User user) {
        return new CreateUserResponseDto(
            user.getId(),
            user.getEmail(),
            user.getStatus().name(),
            user.getCreatedAt()
        );
    }
}
