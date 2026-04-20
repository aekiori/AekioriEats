package com.delivery.payment.infra.portone;

public record PortOnePaymentCancellation(
    String paymentId,
    String cancellationId,
    String status,
    Integer amount
) {
}
