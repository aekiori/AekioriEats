package com.delivery.payment.infra.portone;

public record PortOnePaymentVerification(
    String paymentId,
    String status,
    Integer amount,
    String currency
) {
    public boolean isPaid() {
        return "PAID".equals(status);
    }
}
