package com.delivery.user.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record KafkaDltReadRequestDto(
    @NotBlank String topic,
    @Min(0) Integer partition,
    @Min(0) Long offset,
    @Min(1) @Max(100) Integer limit
) {
    public int resolvedPartition() {
        return partition == null ? 0 : partition;
    }

    public long resolvedOffset() {
        return offset == null ? 0 : offset;
    }

    public int resolvedLimit() {
        return limit == null ? 20 : limit;
    }
}
