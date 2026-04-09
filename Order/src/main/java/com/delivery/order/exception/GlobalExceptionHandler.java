package com.delivery.order.exception;

import com.delivery.order.domain.order.exception.InvalidOrderStatusTransitionException;
import jakarta.validation.ConstraintViolation;
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
        return errorResponse(
            HttpStatus.CONFLICT,
            request,
            "INVALID_ORDER_STATUS_TRANSITION",
            exception.getMessage(),
            null
        );
    }

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApiException(
        ApiException exception,
        HttpServletRequest request
    ) {
        return errorResponse(
            exception.getStatus(),
            request,
            exception.getCode(),
            exception.getMessage(),
            null
        );
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

        String message = firstErrorMessage(errors, "Request is invalid.");

        return validationError(
            request,
            message,
            errors
        );
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolationException(
        ConstraintViolationException exception,
        HttpServletRequest request
    ) {
        String message = exception.getConstraintViolations().stream()
            .map(ConstraintViolation::getMessage)
            .findFirst()
            .orElse("Request is invalid.");

        List<ValidationException.FieldErrorDetail> errors = exception.getConstraintViolations().stream()
            .map(violation -> new ValidationException.FieldErrorDetail(
                violation.getPropertyPath() != null ? violation.getPropertyPath().toString() : "request",
                violation.getMessage()
            ))
            .toList();

        return validationError(request, message, errors);
    }

    @ExceptionHandler({
        HandlerMethodValidationException.class,
        MethodArgumentTypeMismatchException.class,
        MissingRequestHeaderException.class,
        HttpMessageNotReadableException.class
    })
    public ResponseEntity<ErrorResponse> handleBadRequestExceptions(
        Exception exception,
        HttpServletRequest request
    ) {
        return validationError(request, "Request is invalid.", null);
    }

    private ResponseEntity<ErrorResponse> validationError(
        HttpServletRequest request,
        String message,
        List<ValidationException.FieldErrorDetail> errors
    ) {
        return errorResponse(HttpStatus.BAD_REQUEST, request, "VALIDATION_ERROR", message, errors);
    }

    private ResponseEntity<ErrorResponse> errorResponse(
        HttpStatus status,
        HttpServletRequest request,
        String code,
        String message,
        List<ValidationException.FieldErrorDetail> errors
    ) {
        return ResponseEntity.status(status).body(buildErrorResponse(request, code, message, errors));
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

    private String firstErrorMessage(
        List<ValidationException.FieldErrorDetail> errors,
        String defaultMessage
    ) {
        if (errors == null || errors.isEmpty()) {
            return defaultMessage;
        }

        for (ValidationException.FieldErrorDetail error : errors) {
            if (error.reason() != null && !error.reason().isBlank()) {
                return error.reason();
            }
        }

        return defaultMessage;
    }
}
