package com.delivery.order.dto.response;

import com.delivery.order.domain.outbox.Outbox;

import java.time.LocalDateTime;

public record OutboxReplayResponseDto(
    String eventId,
    String status,
    String topic,
    LocalDateTime replayedAt
) {
    public static OutboxReplayResponseDto from(Outbox outbox, String topic, LocalDateTime replayedAt) {
        return new OutboxReplayResponseDto(
            outbox.getEventId(),
            Outbox.Status.INIT.name(),
            topic,
            replayedAt
        );
    }
}
