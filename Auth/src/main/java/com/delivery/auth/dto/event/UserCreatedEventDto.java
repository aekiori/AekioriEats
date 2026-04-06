package com.delivery.auth.dto.event;

import com.delivery.auth.constant.AuthEventType;
import com.delivery.auth.domain.user.event.UserCreatedOutboxEvent;

import java.time.LocalDateTime;

public record UserCreatedEventDto(
    String eventId,
    String eventType,
    LocalDateTime occurredAt,
    Long userId,
    String email,
    String status
) {
    public static UserCreatedEventDto from(
        UserCreatedOutboxEvent event,
        String eventId,
        LocalDateTime occurredAt
    ) {
        return new UserCreatedEventDto(
            eventId,
            AuthEventType.USER_CREATED,
            occurredAt,
            event.userId(),
            event.email(),
            event.status()
        );
    }
}
