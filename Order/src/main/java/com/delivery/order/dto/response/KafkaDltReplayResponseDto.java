package com.delivery.order.dto.response;

public record KafkaDltReplayResponseDto(
    String sourceTopic,
    int sourcePartition,
    long sourceOffset,
    String targetTopic,
    String key
) {
}
