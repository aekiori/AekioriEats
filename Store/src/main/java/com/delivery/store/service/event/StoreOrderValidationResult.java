package com.delivery.store.service.event;

public record StoreOrderValidationResult(
    boolean accepted,
    String code,
    String message
) {
    public static StoreOrderValidationResult pass() {
        return new StoreOrderValidationResult(
            true,
            "OK",
            "Order satisfies store validation rules."
        );
    }

    public static StoreOrderValidationResult reject(String code, String message) {
        return new StoreOrderValidationResult(false, code, message);
    }
}
