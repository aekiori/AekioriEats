package com.delivery.payment.dto.request;

public record ConfirmPaymentRequest(
    Long orderId,
    String paymentId,
    Integer amount
) {
}
