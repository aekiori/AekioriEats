package com.delivery.order.dto.response;

import com.delivery.order.domain.order.OrderItem;

public record OrderItemResponseDto(
    Long menuId,
    String menuName,
    Integer unitPrice,
    Integer quantity,
    Integer lineAmount
) {
    public static OrderItemResponseDto from(OrderItem orderItem) {
        return new OrderItemResponseDto(
            orderItem.getMenuId(),
            orderItem.getMenuName(),
            orderItem.getUnitPrice(),
            orderItem.getQuantity(),
            orderItem.getLineAmount()
        );
    }
}
