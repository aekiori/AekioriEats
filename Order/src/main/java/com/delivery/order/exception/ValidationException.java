package com.delivery.order.exception;

import lombok.Getter;

import java.util.List;

@Getter
public class ValidationException extends RuntimeException {
    private final List<FieldErrorDetail> errors;

    public ValidationException(String message, List<FieldErrorDetail> errors) {
        super(message);
        this.errors = errors;
    }

    public record FieldErrorDetail(
        String field,
        String reason
    ) {
    }
}
