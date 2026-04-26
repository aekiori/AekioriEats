package com.delivery.order.constant;

public final class OrderStatusChangeReason {
    public static final String ORDER_CREATED = "Order created.";
    public static final String STORE_VALIDATION_PASSED = "Store validation passed.";
    public static final String STORE_VALIDATION_REJECTED = "Store validation rejected the order.";
    public static final String PAYMENT_SUCCEEDED = "Payment succeeded.";
    public static final String PAYMENT_FAILED = "Payment failed.";
    public static final String PAYMENT_REFUNDED = "Payment refunded.";
    public static final String STORE_ACCEPTED_PAID_ORDER = "Store accepted the paid order.";
    public static final String STORE_REJECTED_PAID_ORDER = "Store rejected the paid order.";
    public static final String STORE_VALIDATION_TIMEOUT = "Store validation timed out.";
    public static final String PAYMENT_RESULT_TIMEOUT = "Payment result timed out.";
    public static final String STORE_DECISION_TIMEOUT = "Store decision timed out.";

    private OrderStatusChangeReason() {
    }
}
