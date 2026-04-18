package com.delivery.point.service.event;

import com.delivery.point.constant.PointEventType;
import com.delivery.point.dto.event.PointDeductionRequestedEventDto;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class PointDeductionRequestedEventConsumer {
    private final ObjectMapper objectMapper;
    private final PointDeductionRequestedEventHandler pointDeductionRequestedEventHandler;

    @KafkaListener(
        topics = "${point.payment-event.point-deduction-requested-topic:outbox.event.PointDeductionRequested}",
        groupId = "${point.payment-event.point-deduction-consumer-group:point-deduction}"
    )
    public void consume(ConsumerRecord<String, String> record) {
        try {
            PointDeductionRequestedEventDto event = extractEvent(record);
            if (event == null || !PointEventType.POINT_DEDUCTION_REQUESTED.equals(event.eventType())) {
                return;
            }
            pointDeductionRequestedEventHandler.handle(event);
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }

    private PointDeductionRequestedEventDto extractEvent(ConsumerRecord<String, String> record) throws Exception {
        if (record.value() == null || record.value().isBlank()) {
            return null;
        }

        JsonNode root = objectMapper.readTree(record.value());
        PointDeductionRequestedEventDto fromDebeziumEnvelope = extractEventFromDebeziumEnvelope(root);
        if (fromDebeziumEnvelope != null) {
            return fromDebeziumEnvelope;
        }

        return extractEventFromSmtOrDirectPayload(root, findHeaderValue(record, "eventType"));
    }

    private PointDeductionRequestedEventDto extractEventFromDebeziumEnvelope(JsonNode root) throws Exception {
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

        return toEventDto(eventPayloadNode, eventType);
    }

    private PointDeductionRequestedEventDto extractEventFromSmtOrDirectPayload(JsonNode root, String headerEventType) throws Exception {
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

        return toEventDto(eventPayloadNode, eventType);
    }

    private PointDeductionRequestedEventDto toEventDto(JsonNode node, String eventType) {
        String eventId = textValue(node, "eventId");
        Integer schemaVersion = intValue(node, "schemaVersion");
        LocalDateTime occurredAt = localDateTimeValue(node, "occurredAt");
        Long orderId = longValue(node, "orderId");
        Long paymentId = longValue(node, "paymentId");
        Long userId = longValue(node, "userId");
        Integer amount = intValue(node, "amount");

        if (eventId == null || orderId == null || userId == null || amount == null) {
            return null;
        }
        if (schemaVersion == null) {
            schemaVersion = 1;
        }

        return new PointDeductionRequestedEventDto(
            eventId,
            eventType,
            schemaVersion,
            occurredAt,
            orderId,
            paymentId,
            userId,
            amount
        );
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
