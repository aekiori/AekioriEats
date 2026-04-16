package com.delivery.order.constant;

public class OrderEventType {
    public static final String ORDER_CREATED = "OrderCreated";
    public static final String ORDER_STATUS_CHANGED = "OrderStatusChanged";
    public static final String ORDER_VALIDATED = "OrderValidated";
    public static final String ORDER_REJECTED = "OrderRejected";
    public static final String PAYMENT_REQUESTED = "PaymentRequested";

    private OrderEventType() {
    }
}
