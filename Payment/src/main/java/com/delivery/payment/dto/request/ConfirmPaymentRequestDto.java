package com.delivery.payment.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

public record ConfirmPaymentRequestDto(
    @Schema(description = "결제를 확정할 주문 ID", example = "10")
    Long orderId,

    @Schema(description = "클라이언트 paymentId", example = "563dfd39-b007-4760-bfdd-2e71f54d8c49")
    String paymentId,

    @Schema(description = "요청 결제 금액", example = "24400")
    Integer amount
) {
}
