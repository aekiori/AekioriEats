package com.delivery.payment.dto.response;

public record KafkaDltReplayResponseDto(
    String sourceTopic,
    int sourcePartition,
    long sourceOffset,
    String targetTopic,
    String key
) {
}
