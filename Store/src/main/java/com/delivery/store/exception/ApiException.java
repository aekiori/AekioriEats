package com.delivery.store.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class ApiException extends RuntimeException {
    private final String code;
    private final HttpStatus status;

    public ApiException(StoreErrorCode errorCode) {
        this(errorCode, errorCode.message());
    }

    public ApiException(StoreErrorCode errorCode, String message) {
        this(errorCode.code(), message, errorCode.status());
    }

    public ApiException(String code, String message, HttpStatus status) {
        super(message);
        this.code = code;
        this.status = status;
    }
}
