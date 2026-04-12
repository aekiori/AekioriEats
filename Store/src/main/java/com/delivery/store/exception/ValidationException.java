package com.delivery.store.exception;

public class ValidationException {
    public record FieldErrorDetail(
        String field,
        String reason
    ) {
    }
}
