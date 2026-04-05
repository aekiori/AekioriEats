package com.delivery.order.dto.response;

import java.time.LocalDateTime;

public record UpdateOrderStatusResultDto(
    Long orderId,
    String status,
    LocalDateTime updatedAt
) {
}
