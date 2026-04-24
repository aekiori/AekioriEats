package com.delivery.order.dto.request;

import com.delivery.order.domain.order.Order;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record UpdateOrderStatusDto(
    @Schema(description = "변경할 주문 상태", example = "CANCELLED")
    @NotNull
    Order.Status status,

    @Schema(description = "상태 변경 사유", example = "사용자 요청 취소")
    String reason
) {
}
