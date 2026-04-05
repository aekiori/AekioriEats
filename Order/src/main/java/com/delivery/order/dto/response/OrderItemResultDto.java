package com.delivery.order.dto.response;

import com.delivery.order.domain.order.OrderItem;

public record OrderItemResultDto(
    Long menuId,
    String menuName,
    Integer unitPrice,
    Integer quantity,
    Integer lineAmount
) {
    public static OrderItemResultDto from(OrderItem orderItem) {
        return new OrderItemResultDto(
            orderItem.getMenuId(),
            orderItem.getMenuName(),
            orderItem.getUnitPrice(),
            orderItem.getQuantity(),
            orderItem.getLineAmount()
        );
    }
}
