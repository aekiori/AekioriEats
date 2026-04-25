package com.delivery.store.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record StoreSearchRequestDto(
    String q,
    @Min(0) Integer page,
    @Min(1) @Max(100) Integer size
) {
    public String resolvedQuery() {
        return q == null ? "" : q;
    }

    public int resolvedPage() {
        return page == null ? 0 : page;
    }

    public int resolvedSize() {
        return size == null ? 20 : size;
    }
}
