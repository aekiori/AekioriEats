package com.delivery.order.constant;

public class OrderEventType {
    public static final String ORDER_CREATED = "OrderCreated";
    public static final String ORDER_STATUS_CHANGED = "OrderStatusChanged";
    public static final String ORDER_VALIDATED = "OrderValidated";
    public static final String ORDER_REJECTED = "OrderRejected";
    public static final String PAYMENT_REQUESTED = "PaymentRequested";
    public static final String PAYMENT_SUCCEEDED = "PaymentSucceeded";
    public static final String PAYMENT_FAILED = "PaymentFailed";
    public static final String PAYMENT_REFUNDED = "PaymentRefunded";
    public static final String STORE_ORDER_ACCEPTED = "StoreOrderAccepted";
    public static final String STORE_ORDER_REJECTED = "StoreOrderRejected";

    private OrderEventType() {
    }
}
