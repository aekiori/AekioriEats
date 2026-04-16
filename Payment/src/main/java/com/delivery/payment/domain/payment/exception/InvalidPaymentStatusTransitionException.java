package com.delivery.payment.domain.payment.exception;

import com.delivery.payment.domain.payment.Payment;

public class InvalidPaymentStatusTransitionException extends RuntimeException {
    public InvalidPaymentStatusTransitionException(Payment.Status currentStatus, Payment.Status targetStatus) {
        super("Invalid payment status transition. currentStatus=" + currentStatus + ", targetStatus=" + targetStatus);
    }
}
