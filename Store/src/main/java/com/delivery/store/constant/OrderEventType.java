package com.delivery.store.constant;

public final class OrderEventType {
    public static final String ORDER_CREATED = "OrderCreated";
    public static final String ORDER_VALIDATED = "OrderValidated";
    public static final String ORDER_REJECTED = "OrderRejected";

    private OrderEventType() {
    }
}
