package com.delivery.auth.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class ApiException extends RuntimeException {
    private final String code;
    private final HttpStatus status;

    public ApiException(AuthErrorCode errorCode) {
        this(errorCode, errorCode.message());
    }

    public ApiException(AuthErrorCode errorCode, String message) {
        this(errorCode.code(), message, errorCode.status());
    }

    public ApiException(String code, String message, HttpStatus status) {
        super(message);
        this.code = code;
        this.status = status;
    }
}
