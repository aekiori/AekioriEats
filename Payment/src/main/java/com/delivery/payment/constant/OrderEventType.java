package com.delivery.payment.constant;

public final class OrderEventType {
    public static final String PAYMENT_REQUESTED = "PaymentRequested";
    public static final String ORDER_STATUS_CHANGED = "OrderStatusChanged";

    private OrderEventType() {
    }
}
