package com.delivery.store.dto.response;

public record KafkaDltReplayResponseDto(
    String sourceTopic,
    int sourcePartition,
    long sourceOffset,
    String targetTopic,
    String key
) {
}
