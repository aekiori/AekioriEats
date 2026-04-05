package com.delivery.order.dto.event;

import java.time.LocalDateTime;
import java.util.List;

public record OrderCreatedEventDto(
    String eventId,
    String eventType,
    LocalDateTime occurredAt,
    Long orderId,
    Long userId,
    Long storeId,
    Integer totalAmount,
    Integer usedPointAmount,
    Integer finalAmount,
    String status,
    List<OrderCreatedItemDto> items
) {
    public record OrderCreatedItemDto(
        Long menuId,
        String menuName,
        Integer unitPrice,
        Integer quantity,
        Integer lineAmount
    ) {
    }
}
