package com.delivery.user.dto.response;

public record KafkaDltReplayResponseDto(
    String sourceTopic,
    int sourcePartition,
    long sourceOffset,
    String targetTopic,
    String key
) {
}
