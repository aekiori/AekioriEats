package com.delivery.payment.dto.response;

public record ConfirmPaymentResponse(
    Long orderId,
    Long paymentId,
    String providerPaymentId,
    String status,
    Integer amount,
    boolean providerVerified
) {
}
