package com.delivery.order.exception;

import org.springframework.http.HttpStatus;

public enum OrderErrorCode {
    IDEMPOTENT_REQUEST_IN_PROGRESS(
        "Same idempotent request is already being processed.",
        HttpStatus.CONFLICT
    ),
    INVALID_AMOUNT("Final amount must be zero or greater.", HttpStatus.BAD_REQUEST),
    IDEMPOTENCY_KEY_CONFLICT(
        "Different request payload was submitted with the same idempotencyKey.",
        HttpStatus.CONFLICT
    ),
    REQUEST_HASH_GENERATION_ERROR(
        "Request hash generation failed.",
        HttpStatus.INTERNAL_SERVER_ERROR
    ),
    UNAUTHORIZED_PRINCIPAL(
        "Authenticated principal is missing or invalid.",
        HttpStatus.UNAUTHORIZED
    ),
    FORBIDDEN_RESOURCE_ACCESS(
        "You do not have permission to access this resource.",
        HttpStatus.FORBIDDEN
    ),
    ORDER_NOT_FOUND("Order was not found.", HttpStatus.NOT_FOUND),
    OUTBOX_PAYLOAD_SERIALIZATION_ERROR(
        "Outbox payload serialization failed.",
        HttpStatus.INTERNAL_SERVER_ERROR
    ),
    OUTBOX_NOT_FOUND("Outbox event was not found.", HttpStatus.NOT_FOUND),
    TEST_BAD_REQUEST("Intentional bad request for metrics test.", HttpStatus.BAD_REQUEST);

    private final String message;
    private final HttpStatus status;

    OrderErrorCode(String message, HttpStatus status) {
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
