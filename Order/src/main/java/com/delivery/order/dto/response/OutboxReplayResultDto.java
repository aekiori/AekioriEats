package com.delivery.order.dto.response;

import java.time.LocalDateTime;

public record OutboxReplayResultDto(
    String eventId,
    String status,
    String topic,
    LocalDateTime replayedAt
) {
}
