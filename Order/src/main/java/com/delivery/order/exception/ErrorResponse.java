package com.delivery.order.exception;

import java.time.LocalDateTime;
import java.util.List;

public record ErrorResponse(
    LocalDateTime timestamp,
    String path,
    String traceId,
    String code,
    String message,
    List<ValidationException.FieldErrorDetail> errors
) {
}
