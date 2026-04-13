package com.delivery.user.service;

import com.delivery.user.constant.UserEventType;
import com.delivery.user.dto.event.UserCreatedEventDto;
import com.delivery.user.exception.UnprocessableEventException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserCreatedEventConsumer {
    private final ObjectMapper objectMapper;
    private final UserProjectionService userProjectionService;

    @KafkaListener(
        topics = {
            "${user.event.source-topic:delivery.delivery_auth.outbox}",
            "${user.event.smt-topic:outbox.event.USER}"
        },
        groupId = "${user.event.user-created-consumer-group:user-created-projection}"
    )
    public void consume(ConsumerRecord<String, String> record) {
        try {
            UserCreatedEventDto event = extractEvent(record);

            if (event == null) {
                log.debug("UserCreated parse skipped. topic={}, offset={}", record.topic(), record.offset());
                return;
            }

            if (!UserEventType.USER_CREATED.equals(event.eventType())) {
                log.debug(
                    "Unsupported eventType for user projection. topic={}, eventType={}, offset={}",
                    record.topic(),
                    event.eventType(),
                    record.offset()
                );
                return;
            }

            boolean applied = userProjectionService.upsertUserCreated(event);

            if (!applied) {
                log.info(
                    "UserCreated duplicate event skipped. topic={}, eventId={}, userId={}, offset={}",
                    record.topic(),
                    event.eventId(),
                    event.userId(),
                    record.offset()
                );
                return;
            }

            log.info(
                "User projection upserted from event. topic={}, eventId={}, userId={}, schemaVersion={}, occurredAt={}, offset={}",
                record.topic(),
                event.eventId(),
                event.userId(),
                event.schemaVersion(),
                event.occurredAt(),
                record.offset()
            );
        } catch (UnprocessableEventException exception) {
            log.warn(
                "UserCreated event unprocessable, routing to DLQ. topic={}, offset={}, key={}, reason={}",
                record.topic(),
                record.offset(),
                record.key(),
                exception.getMessage()
            );
            throw exception;
        } catch (Exception exception) {
            log.error(
                "UserCreated consume failed. topic={}, offset={}, key={}",
                record.topic(),
                record.offset(),
                record.key(),
                exception
            );
            throw new RuntimeException(exception);
        }
    }

    private UserCreatedEventDto extractEvent(ConsumerRecord<String, String> record) throws Exception {
        JsonNode root = objectMapper.readTree(record.value());

        UserCreatedEventDto fromDebeziumEnvelope = extractEventFromDebeziumEnvelope(root);
        if (fromDebeziumEnvelope != null) {
            return fromDebeziumEnvelope;
        }

        return extractEventFromSmtOrDirectPayload(root, findHeaderValue(record, "eventType"));
    }

    private UserCreatedEventDto extractEventFromDebeziumEnvelope(JsonNode root) throws Exception {
        JsonNode after = root.path("payload").path("after");

        if (after.isMissingNode() || after.isNull()) {
            return null;
        }

        String eventType = textValue(after, "event_type");
        if (eventType == null) {
            return null;
        }

        JsonNode payloadNode = after.path("payload");
        if (payloadNode.isMissingNode() || payloadNode.isNull()) {
            return null;
        }

        JsonNode eventPayloadNode = payloadNode;
        if (payloadNode.isTextual()) {
            eventPayloadNode = objectMapper.readTree(payloadNode.asText());
        }

        return toUserCreatedEventDto(eventPayloadNode, eventType);
    }

    private UserCreatedEventDto extractEventFromSmtOrDirectPayload(JsonNode root, String headerEventType) throws Exception {
        JsonNode eventPayloadNode = root;

        JsonNode payloadNode = root.path("payload");
        if (!payloadNode.isMissingNode() && !payloadNode.isNull()) {
            eventPayloadNode = payloadNode;

            if (payloadNode.isTextual()) {
                eventPayloadNode = objectMapper.readTree(payloadNode.asText());
            }
        }

        String eventType = headerEventType;
        if (eventType == null) {
            eventType = textValue(eventPayloadNode, "eventType");
        }

        if (eventType == null) {
            return null;
        }

        return toUserCreatedEventDto(eventPayloadNode, eventType);
    }

    private UserCreatedEventDto toUserCreatedEventDto(JsonNode node, String eventType) {
        String eventId = textValue(node, "eventId");
        Long userId = longValue(node, "userId");
        String email = textValue(node, "email");
        String status = textValue(node, "status");
        Integer schemaVersion = intValue(node, "schemaVersion");
        LocalDateTime occurredAt = localDateTimeValue(node, "occurredAt");

        if (eventId == null || userId == null || email == null || status == null) {
            return null;
        }

        if (schemaVersion == null) {
            schemaVersion = 1;
        }

        return new UserCreatedEventDto(eventId, eventType, schemaVersion, occurredAt, userId, email, status);
    }

    private String findHeaderValue(ConsumerRecord<String, String> record, String headerName) {
        Header matchedHeader = null;

        for (Header header : record.headers()) {
            if (headerName.equals(header.key())) {
                matchedHeader = header;
            }
        }

        if (matchedHeader == null || matchedHeader.value() == null) {
            return null;
        }

        return new String(matchedHeader.value(), StandardCharsets.UTF_8);
    }

    private String textValue(JsonNode node, String fieldName) {
        JsonNode valueNode = node.path(fieldName);
        if (valueNode.isMissingNode() || valueNode.isNull()) {
            return null;
        }
        return valueNode.asText();
    }

    private Long longValue(JsonNode node, String fieldName) {
        JsonNode valueNode = node.path(fieldName);
        if (valueNode.isMissingNode() || valueNode.isNull()) {
            return null;
        }

        if (valueNode.isNumber()) {
            return valueNode.asLong();
        }

        if (valueNode.isTextual()) {
            try {
                return Long.parseLong(valueNode.asText());
            } catch (NumberFormatException exception) {
                return null;
            }
        }

        return null;
    }

    private Integer intValue(JsonNode node, String fieldName) {
        JsonNode valueNode = node.path(fieldName);
        if (valueNode.isMissingNode() || valueNode.isNull()) {
            return null;
        }

        if (valueNode.isNumber()) {
            return valueNode.asInt();
        }

        if (valueNode.isTextual()) {
            try {
                return Integer.parseInt(valueNode.asText());
            } catch (NumberFormatException exception) {
                return null;
            }
        }

        return null;
    }

    private LocalDateTime localDateTimeValue(JsonNode node, String fieldName) {
        String value = textValue(node, fieldName);
        if (value == null) {
            return null;
        }

        try {
            return LocalDateTime.parse(value);
        } catch (Exception exception) {
            return null;
        }
    }
}
