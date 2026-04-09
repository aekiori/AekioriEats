package com.delivery.user.dto.event;

import java.time.LocalDateTime;

public record UserCreatedEventDto(
    String eventId,
    String eventType,
    int schemaVersion,
    LocalDateTime occurredAt,
    Long userId,
    String email,
    String status
) {
}
