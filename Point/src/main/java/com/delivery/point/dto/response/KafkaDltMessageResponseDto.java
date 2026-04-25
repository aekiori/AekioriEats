package com.delivery.point.dto.response;

import java.util.List;

public record KafkaDltMessageResponseDto(
    String topic,
    int partition,
    long offset,
    String key,
    String value,
    String timestamp,
    List<KafkaDltHeaderResponseDto> headers
) {
}
