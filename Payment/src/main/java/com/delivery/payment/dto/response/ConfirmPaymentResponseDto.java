package com.delivery.payment.dto.response;

public record ConfirmPaymentResponseDto(
    Long orderId,
    Long paymentId,
    String providerPaymentId,
    String status,
    Integer amount,
    boolean providerVerified
) {
}
