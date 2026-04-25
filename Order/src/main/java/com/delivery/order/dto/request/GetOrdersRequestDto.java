package com.delivery.order.dto.request;

import com.delivery.order.domain.order.Order;
import jakarta.validation.constraints.Min;

public record GetOrdersRequestDto(
    Order.Status status,
    @Min(0) Integer page,
    @Min(1) Integer limit
) {
    public int resolvedPage() {
        return page == null ? 0 : page;
    }

    public int resolvedLimit() {
        return limit == null ? 20 : limit;
    }
}
