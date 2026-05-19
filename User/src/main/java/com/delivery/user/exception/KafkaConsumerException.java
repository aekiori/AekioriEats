package com.delivery.user.exception;

public class KafkaConsumerException extends RuntimeException {
    public KafkaConsumerException(String message, Throwable cause) {
        super(message, cause);
    }
}
