package com.delivery.order.dto.response;

import org.springframework.data.domain.Page;

import java.util.List;

public record OrderPageResponseDto(
    List<OrderSummaryResponseDto> content,
    int page,
    int limit,
    long totalElements,
    int totalPages
) {
    public static OrderPageResponseDto from(Page<?> page, List<OrderSummaryResponseDto> content) {
        return new OrderPageResponseDto(
            content,
            page.getNumber(),
            page.getSize(),
            page.getTotalElements(),
            page.getTotalPages()
        );
    }
}
