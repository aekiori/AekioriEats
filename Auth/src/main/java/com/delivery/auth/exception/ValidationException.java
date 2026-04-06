package com.delivery.auth.exception;

public class ValidationException {
    public record FieldErrorDetail(
        String field,
        String reason
    ) {
    }
}
