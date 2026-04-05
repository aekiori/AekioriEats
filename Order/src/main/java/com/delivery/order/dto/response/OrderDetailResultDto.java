package com.delivery.order.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record OrderDetailResultDto(
    Long orderId,
    Long userId,
    Long storeId,
    String status,
    String deliveryAddress,
    Integer totalAmount,
    Integer usedPointAmount,
    Integer finalAmount,
    List<OrderItemResultDto> items,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
}
