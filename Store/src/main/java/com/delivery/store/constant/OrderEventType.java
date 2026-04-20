package com.delivery.store.constant;

public final class OrderEventType {
    public static final String ORDER_CREATED = "OrderCreated";
    public static final String ORDER_STATUS_CHANGED = "OrderStatusChanged";
    public static final String ORDER_VALIDATED = "OrderValidated";
    public static final String ORDER_REJECTED = "OrderRejected";
    public static final String STORE_ORDER_ACCEPTED = "StoreOrderAccepted";
    public static final String STORE_ORDER_REJECTED = "StoreOrderRejected";

    private OrderEventType() {
    }
}
