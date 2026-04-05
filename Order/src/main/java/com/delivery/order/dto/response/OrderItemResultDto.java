package com.delivery.order.dto.response;

public record OrderItemResultDto(
    Long menuId,
    String menuName,
    Integer unitPrice,
    Integer quantity,
    Integer lineAmount
) {
}
