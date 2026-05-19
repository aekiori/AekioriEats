package com.delivery.order.exception;

public class KafkaConsumerException extends RuntimeException {
    public KafkaConsumerException(String message, Throwable cause) {
        super(message, cause);
    }
}
