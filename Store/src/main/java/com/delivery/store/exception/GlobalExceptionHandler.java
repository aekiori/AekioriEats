package com.delivery.store.exception;

import com.delivery.store.domain.store.exception.InvalidStoreStatusTransitionException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApiException(ApiException exception, HttpServletRequest request) {
        return response(exception.getStatus(), request, exception.getCode(), exception.getMessage(), null);
    }

    @ExceptionHandler(InvalidStoreStatusTransitionException.class)
    public ResponseEntity<ErrorResponse> handleInvalidTransition(
        InvalidStoreStatusTransitionException exception,
        HttpServletRequest request
    ) {
        return response(
            HttpStatus.CONFLICT,
            request,
            "INVALID_STORE_STATUS_TRANSITION",
            exception.getMessage(),
            null
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(
        MethodArgumentNotValidException exception,
        HttpServletRequest request
    ) {
        List<ValidationException.FieldErrorDetail> errors = exception.getBindingResult()
            .getFieldErrors()
            .stream()
            .map(this::toFieldErrorDetail)
            .toList();

        return response(HttpStatus.BAD_REQUEST, request, "VALIDATION_ERROR", "Request is invalid.", errors);
    }

    @ExceptionHandler({
        ConstraintViolationException.class,
        MethodArgumentTypeMismatchException.class,
        HttpMessageNotReadableException.class
    })
    public ResponseEntity<ErrorResponse> handleBadRequest(Exception exception, HttpServletRequest request) {
        return response(HttpStatus.BAD_REQUEST, request, "VALIDATION_ERROR", "Request is invalid.", null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception exception, HttpServletRequest request) {
        return response(
            HttpStatus.INTERNAL_SERVER_ERROR,
            request,
            "INTERNAL_SERVER_ERROR",
            "An unexpected error occurred.",
            null
        );
    }

    private ResponseEntity<ErrorResponse> response(
        HttpStatus status,
        HttpServletRequest request,
        String code,
        String message,
        List<ValidationException.FieldErrorDetail> errors
    ) {
        return ResponseEntity.status(status).body(new ErrorResponse(
            LocalDateTime.now(),
            request.getRequestURI(),
            code,
            message,
            errors
        ));
    }

    private ValidationException.FieldErrorDetail toFieldErrorDetail(FieldError error) {
        String reason = error.getDefaultMessage() != null ? error.getDefaultMessage() : "Invalid value.";
        return new ValidationException.FieldErrorDetail(error.getField(), reason);
    }
}
