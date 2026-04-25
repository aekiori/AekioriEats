package com.delivery.point.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

public record ChargePointRequestDto(
    @Schema(description = "충전할 포인트 금액", example = "10000")
    Integer amount,

    @Schema(description = "충전 사유", example = "local test seed")
    String reason
) {
}
