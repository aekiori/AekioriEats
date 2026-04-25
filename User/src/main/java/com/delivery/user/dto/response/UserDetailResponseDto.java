package com.delivery.user.dto.response;

import com.delivery.user.domain.user.User;

import java.time.LocalDateTime;

public record UserDetailResponseDto(
    Long userId,
    String email,
    String status,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    public static UserDetailResponseDto from(User user) {
        return new UserDetailResponseDto(
            user.getId(),
            user.getEmail(),
            user.getStatus().name(),
            user.getCreatedAt(),
            user.getUpdatedAt()
        );
    }
}
