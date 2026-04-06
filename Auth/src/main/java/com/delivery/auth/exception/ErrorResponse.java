package com.delivery.auth.exception;

import java.time.LocalDateTime;
import java.util.List;

public record ErrorResponse(
    LocalDateTime timestamp,
    String path,
    String code,
    String message,
    List<ValidationException.FieldErrorDetail> errors
) {
}
