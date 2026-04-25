package com.delivery.order.dto.response;

import com.delivery.order.domain.order.Order;
import com.delivery.order.domain.order.OrderStatusHistory;

import java.time.LocalDateTime;

public record OrderStatusHistoryResponseDto(
    Order.Status fromStatus,
    Order.Status toStatus,
    String reason,
    OrderStatusHistory.SourceType sourceType,
    String eventId,
    LocalDateTime createdAt
) {
}
