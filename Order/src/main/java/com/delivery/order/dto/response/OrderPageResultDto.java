package com.delivery.order.dto.response;

import java.util.List;

public record OrderPageResultDto(
    List<OrderSummaryResultDto> content,
    int page,
    int size,
    long totalElements,
    int totalPages
) {
}
