package com.delivery.order.dto.response;

import com.delivery.order.domain.outbox.Outbox;

import java.time.LocalDateTime;

public record OutboxResponseDto(
    Long id,
    String eventId,
    String aggregateType,
    Long aggregateId,
    String eventType,
    String status,
    String partitionKey,
    LocalDateTime createdAt
) {
    public static OutboxResponseDto from(Outbox outbox) {
        return new OutboxResponseDto(
            outbox.getId(),
            outbox.getEventId(),
            outbox.getAggregateType().name(),
            outbox.getAggregateId(),
            outbox.getEventType(),
            outbox.getStatus().name(),
            outbox.getPartitionKey(),
            outbox.getCreatedAt()
        );
    }
}
