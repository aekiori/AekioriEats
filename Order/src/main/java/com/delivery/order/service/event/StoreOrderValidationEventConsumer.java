package com.delivery.order.service.event;

import com.delivery.order.constant.OrderEventType;
import com.delivery.order.dto.event.StoreOrderValidationEventDto;
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

@Component
@RequiredArgsConstructor
@Slf4j
public class StoreOrderValidationEventConsumer {
    private final ObjectMapper objectMapper;
    private final StoreOrderValidationResultService storeOrderValidationResultService;

    @KafkaListener(
        topics = {
            "${order.store-validation-event.order-validated-topic:outbox.event.OrderValidated}",
            "${order.store-validation-event.order-rejected-topic:outbox.event.OrderRejected}"
        },
        groupId = "${order.store-validation-event.consumer-group:order-store-validation}"
    )
    public void consume(ConsumerRecord<String, String> record) {
        try {
            StoreOrderValidationEventDto event = extractEvent(record);

            if (event == null) {
                return;
            }

            if (!OrderEventType.ORDER_VALIDATED.equals(event.eventType())
                && !OrderEventType.ORDER_REJECTED.equals(event.eventType())) {
                return;
            }

            storeOrderValidationResultService.handle(event);
        } catch (Exception exception) {
            log.error(
                "Store validation consume failed. topic={}, partition={}, offset={}",
                record.topic(),
                record.partition(),
                record.offset(),
                exception
            );
        }
    }

    private StoreOrderValidationEventDto extractEvent(ConsumerRecord<String, String> record) throws Exception {
        if (record.value() == null || record.value().isBlank()) {
            return null;
        }

        JsonNode root = objectMapper.readTree(record.value());

        StoreOrderValidationEventDto fromDebeziumEnvelope = extractEventFromDebeziumEnvelope(root);
        if (fromDebeziumEnvelope != null) {
            return fromDebeziumEnvelope;
        }

        return extractEventFromSmtOrDirectPayload(root, findHeaderValue(record, "eventType"));
    }

    private StoreOrderValidationEventDto extractEventFromDebeziumEnvelope(JsonNode root) throws Exception {
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

        return toDto(eventPayloadNode, eventType);
    }

    private StoreOrderValidationEventDto extractEventFromSmtOrDirectPayload(JsonNode root, String headerEventType) throws Exception {
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

        return toDto(eventPayloadNode, eventType);
    }

    private StoreOrderValidationEventDto toDto(JsonNode node, String eventType) {
        String eventId = textValue(node, "eventId");
        Integer schemaVersion = intValue(node, "schemaVersion");
        LocalDateTime occurredAt = localDateTimeValue(node, "occurredAt");
        Long orderId = longValue(node, "orderId");
        Long storeId = longValue(node, "storeId");
        String validationResult = textValue(node, "validationResult");
        String rejectCode = textValue(node, "rejectCode");
        String rejectReason = textValue(node, "rejectReason");

        if (eventId == null || orderId == null || storeId == null) {
            return null;
        }

        if (schemaVersion == null) {
            schemaVersion = 1;
        }

        return new StoreOrderValidationEventDto(
            eventId,
            eventType,
            schemaVersion,
            occurredAt,
            orderId,
            storeId,
            validationResult,
            rejectCode,
            rejectReason
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

