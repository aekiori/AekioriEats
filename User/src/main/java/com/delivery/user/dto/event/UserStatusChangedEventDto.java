package com.delivery.user.dto.event;

import com.delivery.user.constant.UserEventType;
import com.delivery.user.domain.user.event.UserStatusChangedOutboxEvent;

import java.time.LocalDateTime;

public record UserStatusChangedEventDto(
    String eventId,
    String eventType,
    LocalDateTime occurredAt,
    Long userId,
    String email,
    String currentStatus,
    String targetStatus,
    String reason
) {
    public static UserStatusChangedEventDto from(
        UserStatusChangedOutboxEvent event,
        String eventId,
        LocalDateTime occurredAt
    ) {
        return new UserStatusChangedEventDto(
            eventId,
            UserEventType.USER_STATUS_CHANGED,
            occurredAt,
            event.userId(),
            event.email(),
            event.currentStatus().name(),
            event.targetStatus().name(),
            event.reason()
        );
    }
}
