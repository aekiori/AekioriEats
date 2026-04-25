package com.delivery.auth.dto.response;

public record EmailDuplicateCheckResponseDto(
    String email,
    boolean exists
) {
    public static EmailDuplicateCheckResponseDto from(String email, boolean exists) {
        return new EmailDuplicateCheckResponseDto(email, exists);
    }
}
