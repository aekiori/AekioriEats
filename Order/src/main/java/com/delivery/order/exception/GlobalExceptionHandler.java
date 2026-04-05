package com.delivery.order.exception;

import com.delivery.order.domain.order.exception.InvalidOrderStatusTransitionException;
import jakarta.validation.ConstraintViolationException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.validation.FieldError;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.MissingRequestHeaderException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(InvalidOrderStatusTransitionException.class)
    public ResponseEntity<ErrorResponse> handleInvalidOrderStatusTransitionException(
        InvalidOrderStatusTransitionException exception,
        HttpServletRequest request
    ) {
        ErrorResponse response = buildErrorResponse(
            request,
            "INVALID_ORDER_STATUS_TRANSITION",
            exception.getMessage(),
            null
        );

        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApiException(
        ApiException exception,
        HttpServletRequest request
    ) {
        ErrorResponse response = buildErrorResponse(
            request,
            exception.getCode(),
            exception.getMessage(),
            null
        );

        return ResponseEntity.status(exception.getStatus()).body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(
        MethodArgumentNotValidException exception,
        HttpServletRequest request
    ) {
        List<ValidationException.FieldErrorDetail> errors = exception.getBindingResult()
            .getFieldErrors()
            .stream()
            .map(this::toFieldErrorDetail)
            .toList();

        ErrorResponse response = buildErrorResponse(
            request,
            "VALIDATION_ERROR",
            "Request is invalid.",
            errors
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ErrorResponse> handleHandlerMethodValidationException(
        HandlerMethodValidationException exception,
        HttpServletRequest request
    ) {
        ErrorResponse response = buildErrorResponse(
            request,
            "VALIDATION_ERROR",
            "Request is invalid.",
            null
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolationException(
        ConstraintViolationException exception,
        HttpServletRequest request
    ) {
        ErrorResponse response = buildErrorResponse(
            request,
            "VALIDATION_ERROR",
            "Request is invalid.",
            null
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentTypeMismatchException(
        MethodArgumentTypeMismatchException exception,
        HttpServletRequest request
    ) {
        ErrorResponse response = buildErrorResponse(
            request,
            "VALIDATION_ERROR",
            "Request is invalid.",
            null
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ErrorResponse> handleMissingRequestHeaderException(
        MissingRequestHeaderException exception,
        HttpServletRequest request
    ) {
        ErrorResponse response = buildErrorResponse(
            request,
            "VALIDATION_ERROR",
            "Request is invalid.",
            null
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadableException(
        HttpMessageNotReadableException exception,
        HttpServletRequest request
    ) {
        ErrorResponse response = buildErrorResponse(
            request,
            "VALIDATION_ERROR",
            "Request is invalid.",
            null
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    private ErrorResponse buildErrorResponse(
        HttpServletRequest request,
        String code,
        String message,
        List<ValidationException.FieldErrorDetail> errors
    ) {
        return new ErrorResponse(
            LocalDateTime.now(),
            request.getRequestURI(),
            resolveTraceId(request),
            code,
            message,
            errors
        );
    }

    private String resolveTraceId(HttpServletRequest request) {
        Object traceId = request.getAttribute(TraceIdFilter.TRACE_ID_ATTRIBUTE);

        if (traceId instanceof String traceIdValue && !traceIdValue.isBlank()) {
            return traceIdValue;
        }

        String headerTraceId = request.getHeader(TraceIdFilter.TRACE_ID_HEADER);

        if (headerTraceId != null && !headerTraceId.isBlank()) {
            return headerTraceId.trim();
        }

        return UUID.randomUUID().toString();
    }

    private ValidationException.FieldErrorDetail toFieldErrorDetail(FieldError fieldError) {
        String reason = fieldError.getDefaultMessage() != null
            ? fieldError.getDefaultMessage()
            : "Invalid value.";

        return new ValidationException.FieldErrorDetail(fieldError.getField(), reason);
    }
}
