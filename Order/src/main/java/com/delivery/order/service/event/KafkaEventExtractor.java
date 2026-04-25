package com.delivery.order.service.event;

import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class KafkaEventExtractor {
    private final ObjectMapper objectMapper;

    public ExtractedEvent extractEvent(ConsumerRecord<String, String> record) throws Exception {
        if (record.value() == null || record.value().isBlank()) {
            return null;
        }

        JsonNode root = objectMapper.readTree(record.value());

        ExtractedEvent fromDebeziumEnvelope = extractEventFromDebeziumEnvelope(root);
        if (fromDebeziumEnvelope != null) {
            return fromDebeziumEnvelope;
        }

        return extractEventFromSmtOrDirectPayload(root, headerValue(record, "eventType"));
    }

    public String extractOutboxEventId(ConsumerRecord<String, String> record) throws Exception {
        String customEventIdHeader = headerValue(record, "eventId");
        if (customEventIdHeader != null) {
            return customEventIdHeader;
        }

        String eventIdHeader = headerValue(record, "id");
        if (eventIdHeader != null) {
            return eventIdHeader;
        }

        if (record.value() == null || record.value().isBlank()) {
            return null;
        }

        JsonNode root = objectMapper.readTree(record.value());

        String eventIdFromDebeziumEnvelope = extractEventIdFromDebeziumEnvelope(root);
        if (eventIdFromDebeziumEnvelope != null) {
            return eventIdFromDebeziumEnvelope;
        }

        ExtractedEvent event = extractEventFromSmtOrDirectPayload(root, null);
        return event != null ? textValue(event.payload(), "eventId") : null;
    }

    private ExtractedEvent extractEventFromDebeziumEnvelope(JsonNode root) throws Exception {
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

        return new ExtractedEvent(readPayloadNode(payloadNode), eventType);
    }

    private ExtractedEvent extractEventFromSmtOrDirectPayload(JsonNode root, String headerEventType) throws Exception {
        JsonNode eventPayloadNode = root;

        JsonNode payloadNode = root.path("payload");
        if (!payloadNode.isMissingNode() && !payloadNode.isNull()) {
            eventPayloadNode = readPayloadNode(payloadNode);
        }

        String eventType = headerEventType;
        if (eventType == null) {
            eventType = textValue(eventPayloadNode, "eventType");
        }

        if (eventType == null) {
            return null;
        }

        return new ExtractedEvent(eventPayloadNode, eventType);
    }

    private String extractEventIdFromDebeziumEnvelope(JsonNode root) {
        JsonNode after = root.path("payload").path("after");
        if (after.isMissingNode() || after.isNull()) {
            return null;
        }

        String status = textValue(after, "status");
        if (!"INIT".equals(status)) {
            return null;
        }

        return textValue(after, "event_id");
    }

    private JsonNode readPayloadNode(JsonNode payloadNode) throws Exception {
        if (payloadNode.isTextual()) {
            return objectMapper.readTree(payloadNode.asText());
        }

        return payloadNode;
    }

    public String headerValue(ConsumerRecord<String, String> record, String headerName) {
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

    public String textValue(JsonNode node, String fieldName) {
        JsonNode valueNode = node.path(fieldName);

        if (valueNode.isMissingNode() || valueNode.isNull()) {
            return null;
        }

        return valueNode.asText();
    }

    public Long longValue(JsonNode node, String fieldName) {
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

    public Integer intValue(JsonNode node, String fieldName) {
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

    public LocalDateTime localDateTimeValue(JsonNode node, String fieldName) {
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

    public record ExtractedEvent(
        JsonNode payload,
        String eventType
    ) {
    }
}
