package com.delivery.order.dto.response;

import org.springframework.data.domain.Page;

import java.util.List;

public record OrderPageResultDto(
    List<OrderSummaryResultDto> content,
    int page,
    int limit,
    long totalElements,
    int totalPages
) {
    public static OrderPageResultDto from(Page<?> page, List<OrderSummaryResultDto> content) {
        return new OrderPageResultDto(
            content,
            page.getNumber(),
            page.getSize(),
            page.getTotalElements(),
            page.getTotalPages()
        );
    }
}
