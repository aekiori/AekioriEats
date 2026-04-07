package com.delivery.auth.dto.event;

import com.delivery.auth.constant.AuthEventType;
import com.delivery.auth.domain.user.event.UserCreatedOutboxEvent;

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
    private static final int CURRENT_SCHEMA_VERSION = 1;

    public static UserCreatedEventDto from(
        UserCreatedOutboxEvent event,
        String eventId,
        LocalDateTime occurredAt
    ) {
        return new UserCreatedEventDto(
            eventId,
            AuthEventType.USER_CREATED,
            CURRENT_SCHEMA_VERSION,
            occurredAt,
            event.userId(),
            event.email(),
            event.status()
        );
    }
}
