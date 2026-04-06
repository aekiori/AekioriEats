package com.delivery.user.exception;

public class ValidationException {
    public record FieldErrorDetail(
        String field,
        String reason
    ) {
    }
}
