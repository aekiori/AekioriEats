package com.delivery.order.dto.response;

import java.time.LocalDateTime;

public record OrderSummaryResultDto(
    Long orderId,
    Long storeId,
    String status,
    Integer finalAmount,
    LocalDateTime createdAt
) {
}
