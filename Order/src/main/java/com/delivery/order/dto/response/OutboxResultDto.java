package com.delivery.order.dto.response;

import com.delivery.order.domain.outbox.Outbox;

import java.time.LocalDateTime;

public record OutboxResultDto(
    Long id,
    String eventId,
    String aggregateType,
    Long aggregateId,
    String eventType,
    String status,
    String partitionKey,
    LocalDateTime createdAt
) {
    public static OutboxResultDto from(Outbox outbox) {
        return new OutboxResultDto(
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
