package com.delivery.store.service.event;

import com.delivery.store.constant.OrderEventType;
import com.delivery.store.dto.event.OrderStatusChangedEventDto;
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
public class OrderPaidEventConsumer {
    private final ObjectMapper objectMapper;
    private final OrderPaidEventHandler orderPaidEventHandler;

    @KafkaListener(
        topics = "${store.order-event.order-status-changed-topic:outbox.event.OrderStatusChanged}",
        groupId = "${store.order-event.order-paid-consumer-group:store-order-paid}"
    )
    public void consume(ConsumerRecord<String, String> record) {
        try {
            OrderStatusChangedEventDto event = extractEvent(record);
            if (event == null || !OrderEventType.ORDER_STATUS_CHANGED.equals(event.eventType())) {
                return;
            }

            orderPaidEventHandler.handle(event);
        } catch (Exception exception) {
            log.error(
                "Order paid consume failed. topic={}, partition={}, offset={}",
                record.topic(),
                record.partition(),
                record.offset(),
                exception
            );
        }
    }

    private OrderStatusChangedEventDto extractEvent(ConsumerRecord<String, String> record) throws Exception {
        if (record.value() == null || record.value().isBlank()) {
            return null;
        }

        JsonNode root = objectMapper.readTree(record.value());
        OrderStatusChangedEventDto fromDebeziumEnvelope = extractEventFromDebeziumEnvelope(root);
        if (fromDebeziumEnvelope != null) {
            return fromDebeziumEnvelope;
        }

        return extractEventFromSmtOrDirectPayload(root, findHeaderValue(record, "eventType"));
    }

    private OrderStatusChangedEventDto extractEventFromDebeziumEnvelope(JsonNode root) throws Exception {
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

    private OrderStatusChangedEventDto extractEventFromSmtOrDirectPayload(JsonNode root, String headerEventType) throws Exception {
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

    private OrderStatusChangedEventDto toDto(JsonNode node, String eventType) {
        String eventId = textValue(node, "eventId");
        Integer schemaVersion = intValue(node, "schemaVersion");
        LocalDateTime occurredAt = localDateTimeValue(node, "occurredAt");
        Long orderId = longValue(node, "orderId");
        Long userId = longValue(node, "userId");
        Long storeId = longValue(node, "storeId");
        Integer totalAmount = intValue(node, "totalAmount");
        Integer usedPointAmount = intValue(node, "usedPointAmount");
        Integer finalAmount = intValue(node, "finalAmount");
        String currentStatus = textValue(node, "currentStatus");
        String targetStatus = textValue(node, "targetStatus");
        String reason = textValue(node, "reason");

        if (eventId == null || orderId == null || storeId == null || targetStatus == null) {
            return null;
        }
        if (schemaVersion == null) {
            schemaVersion = 1;
        }

        return new OrderStatusChangedEventDto(
            eventId,
            eventType,
            schemaVersion,
            occurredAt,
            orderId,
            userId,
            storeId,
            totalAmount,
            usedPointAmount,
            finalAmount,
            currentStatus,
            targetStatus,
            reason
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
