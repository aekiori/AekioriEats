package com.delivery.auth.service.outbox;

import com.delivery.auth.constant.AuthEventType;
import com.delivery.auth.domain.outbox.Outbox;
import com.delivery.auth.domain.user.event.UserCreatedOutboxEvent;
import com.delivery.auth.dto.event.UserCreatedEventDto;
import com.delivery.auth.exception.ApiException;
import com.delivery.auth.repository.outbox.OutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthOutboxService {
    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    public void saveUserCreated(UserCreatedOutboxEvent event) {
        String eventId = getEventId();
        String payload = buildUserCreatedPayload(event, eventId);

        Outbox outbox = new Outbox(
            eventId,
            Outbox.AggregateType.USER,
            event.userId(),
            AuthEventType.USER_CREATED,
            payload,
            Outbox.Status.INIT,
            String.valueOf(event.userId())
        );

        outboxRepository.save(outbox);

        log.info("UserCreated Outbox saved. userId={}, eventId={}", event.userId(), eventId);
    }

    private String buildUserCreatedPayload(UserCreatedOutboxEvent event, String eventId) {
        UserCreatedEventDto payload = UserCreatedEventDto.from(event, eventId, LocalDateTime.now());

        return serializePayload(payload, event.userId());
    }

    private String serializePayload(Object payload, Long userId) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception exception) {
            log.error("Outbox payload serialization failed. userId={}", userId, exception);
            throw new ApiException(
                "OUTBOX_PAYLOAD_SERIALIZATION_ERROR",
                "Outbox payload serialization failed.",
                HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    private String getEventId() {
        return UUID.randomUUID().toString();
    }
}
