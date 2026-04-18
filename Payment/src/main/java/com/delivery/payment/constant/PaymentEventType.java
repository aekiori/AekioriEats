package com.delivery.payment.constant;

public final class PaymentEventType {
    public static final String POINT_DEDUCTION_REQUESTED = "PointDeductionRequested";
    public static final String PAYMENT_SUCCEEDED = "PaymentSucceeded";
    public static final String PAYMENT_FAILED = "PaymentFailed";

    private PaymentEventType() {
    }
}
