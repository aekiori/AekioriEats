package com.delivery.auth.exception;

import org.springframework.http.HttpStatus;

public enum AuthErrorCode {
    EMAIL_ALREADY_EXISTS("Email is already registered.", HttpStatus.CONFLICT),
    INVALID_CREDENTIALS("Email or password is invalid.", HttpStatus.UNAUTHORIZED),
    USER_NOT_ACTIVE("User is not active.", HttpStatus.FORBIDDEN),
    INVALID_REFRESH_TOKEN("Refresh token is invalid.", HttpStatus.UNAUTHORIZED),
    REFRESH_TOKEN_REUSE_DETECTED(
        "Refresh token reuse detected. Please login again.",
        HttpStatus.UNAUTHORIZED
    ),
    REFRESH_TOKEN_EXPIRED("Refresh token is expired.", HttpStatus.UNAUTHORIZED),
    USER_NOT_FOUND("User was not found.", HttpStatus.UNAUTHORIZED),
    RATE_LIMITED("Too many requests. Please try again later.", HttpStatus.TOO_MANY_REQUESTS),
    OUTBOX_PAYLOAD_SERIALIZATION_ERROR(
        "Outbox payload serialization failed.",
        HttpStatus.INTERNAL_SERVER_ERROR
    ),
    TEST_BAD_REQUEST("Intentional bad request for metrics test.", HttpStatus.BAD_REQUEST);

    private final String message;
    private final HttpStatus status;

    AuthErrorCode(String message, HttpStatus status) {
        this.message = message;
        this.status = status;
    }

    public String code() {
        return name();
    }

    public String message() {
        return message;
    }

    public HttpStatus status() {
        return status;
    }
}
