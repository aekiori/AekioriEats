package com.delivery.payment.service.event;

import com.delivery.payment.constant.OrderEventType;
import com.delivery.payment.dto.event.PaymentRequestedEventDto;
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
public class PaymentRequestedEventConsumer {
    private final ObjectMapper objectMapper;
    private final PaymentRequestedEventHandler paymentRequestedEventHandler;

    @KafkaListener(
        topics = {
            "${payment.order-event.source-topic:delivery.delivery_order.outbox}",
            "${payment.order-event.smt-topic:outbox.event.ORDER}"
        },
        groupId = "${payment.order-event.payment-requested-consumer-group:payment-payment-requested}"
    )
    public void consume(ConsumerRecord<String, String> record) {
        try {
            PaymentRequestedEventDto event = extractEvent(record);
            if (event == null || !OrderEventType.PAYMENT_REQUESTED.equals(event.eventType())) {
                return;
            }
            paymentRequestedEventHandler.handle(event);
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }

    private PaymentRequestedEventDto extractEvent(ConsumerRecord<String, String> record) throws Exception {
        if (record.value() == null || record.value().isBlank()) {
            return null;
        }

        JsonNode root = objectMapper.readTree(record.value());

        PaymentRequestedEventDto fromDebeziumEnvelope = extractEventFromDebeziumEnvelope(root);
        if (fromDebeziumEnvelope != null) {
            return fromDebeziumEnvelope;
        }

        return extractEventFromSmtOrDirectPayload(root, findHeaderValue(record, "eventType"));
    }

    private PaymentRequestedEventDto extractEventFromDebeziumEnvelope(JsonNode root) throws Exception {
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

        return toPaymentRequestedEventDto(eventPayloadNode, eventType);
    }

    private PaymentRequestedEventDto extractEventFromSmtOrDirectPayload(JsonNode root, String headerEventType) throws Exception {
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

        return toPaymentRequestedEventDto(eventPayloadNode, eventType);
    }

    private PaymentRequestedEventDto toPaymentRequestedEventDto(JsonNode node, String eventType) {
        String eventId = textValue(node, "eventId");
        Integer schemaVersion = intValue(node, "schemaVersion");
        LocalDateTime occurredAt = localDateTimeValue(node, "occurredAt");
        Long orderId = longValue(node, "orderId");
        Long userId = longValue(node, "userId");
        Long storeId = longValue(node, "storeId");
        Integer totalAmount = intValue(node, "totalAmount");
        Integer usedPointAmount = intValue(node, "usedPointAmount");
        Integer finalAmount = intValue(node, "finalAmount");
        String status = textValue(node, "status");

        if (eventId == null || orderId == null) {
            return null;
        }

        if (schemaVersion == null) {
            schemaVersion = 1;
        }

        return new PaymentRequestedEventDto(
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
            status
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
