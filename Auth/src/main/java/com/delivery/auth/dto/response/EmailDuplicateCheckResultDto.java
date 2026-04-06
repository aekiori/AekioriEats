package com.delivery.auth.dto.response;

public record EmailDuplicateCheckResultDto(
    String email,
    boolean exists
) {
    public static EmailDuplicateCheckResultDto from(String email, boolean exists) {
        return new EmailDuplicateCheckResultDto(email, exists);
    }
}
