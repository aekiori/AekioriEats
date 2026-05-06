package com.delivery.user.exception;

import org.springframework.http.HttpStatus;

public enum UserErrorCode {
    EMAIL_ALREADY_EXISTS("Email is already registered.", HttpStatus.CONFLICT),
    USER_NOT_FOUND("User was not found.", HttpStatus.NOT_FOUND),
    UNAUTHORIZED_PRINCIPAL(
        "Authenticated principal is missing or invalid.",
        HttpStatus.UNAUTHORIZED
    ),
    FORBIDDEN_RESOURCE_ACCESS(
        "You do not have permission to access this resource.",
        HttpStatus.FORBIDDEN
    ),
    TEST_BAD_REQUEST("Intentional bad request for metrics test.", HttpStatus.BAD_REQUEST);

    private final String message;
    private final HttpStatus status;

    UserErrorCode(String message, HttpStatus status) {
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
