package com.delivery.point.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record KafkaDltReplayRequestDto(
    @NotBlank String topic,
    @Min(0) int partition,
    @Min(0) long offset,
    String targetTopic
) {
}
