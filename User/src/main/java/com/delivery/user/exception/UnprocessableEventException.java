package com.delivery.user.exception;

public class UnprocessableEventException extends RuntimeException {
    public UnprocessableEventException(String message) {
        super(message);
    }
}
