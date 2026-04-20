package com.delivery.order.service.outbox;

import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class OutboxPublishStatusConsumer {
    private static final Logger log = LoggerFactory.getLogger(OutboxPublishStatusConsumer.class);

    private final OutboxStatusService outboxStatusService;
    private final ObjectMapper objectMapper;

    @KafkaListener(
        topics = {
            "${order.outbox.order-created-topic:outbox.event.OrderCreated}",
            "${order.outbox.order-status-changed-topic:outbox.event.OrderStatusChanged}",
            "${order.outbox.payment-requested-topic:outbox.event.PaymentRequested}"
        },
        groupId = "${order.outbox.publisher-consumer-group:order-outbox-status}"
    )
    public void consume(ConsumerRecord<String, String> record) {
        try {
            String eventId = extractEventId(record);

            if (eventId == null) {
                log.debug("Outbox eventId 추출 실패. topic={}, offset={}", record.topic(), record.offset());
                return;
            }

            boolean updated = outboxStatusService.markPublishedIfInit(eventId);

            if (updated) {
                log.info(
                    "Outbox 발행 상태 자동 전환 완료. topic={}, eventId={}, offset={}",
                    record.topic(),
                    eventId,
                    record.offset()
                );
                return;
            }

            log.debug(
                "Outbox 상태 자동 전환 스킵. topic={}, eventId={}, offset={}",
                record.topic(),
                eventId,
                record.offset()
            );
        } catch (Exception exception) {
            log.error(
                "Outbox 발행 상태 자동 전환 실패. topic={}, offset={}",
                record.topic(),
                record.offset(),
                exception
            );
        }
    }

    private String extractEventId(ConsumerRecord<String, String> record) throws Exception {
        Header customEventIdHeader = findHeader(record, "eventId");

        if (customEventIdHeader != null) {
            return new String(customEventIdHeader.value(), StandardCharsets.UTF_8);
        }

        Header eventIdHeader = findHeader(record, "id");

        if (eventIdHeader != null) {
            return new String(eventIdHeader.value(), StandardCharsets.UTF_8);
        }

        JsonNode root = objectMapper.readTree(record.value());

        String eventIdFromDebeziumEnvelope = extractEventIdFromDebeziumEnvelope(root);

        if (eventIdFromDebeziumEnvelope != null) {
            return eventIdFromDebeziumEnvelope;
        }

        return extractEventIdFromSmtPayload(root);
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

    private String extractEventIdFromSmtPayload(JsonNode root) throws Exception {
        String directEventId = textValue(root, "eventId");

        if (directEventId != null) {
            return directEventId;
        }

        JsonNode payloadNode = root.path("payload");

        if (payloadNode.isMissingNode() || payloadNode.isNull()) {
            return null;
        }

        JsonNode eventPayloadNode = payloadNode;

        if (payloadNode.isTextual()) {
            eventPayloadNode = objectMapper.readTree(payloadNode.asText());
        }

        return textValue(eventPayloadNode, "eventId");
    }

    private Header findHeader(ConsumerRecord<String, String> record, String headerName) {
        Header matchedHeader = null;

        for (Header header : record.headers()) {
            if (headerName.equals(header.key())) {
                matchedHeader = header;
            }
        }

        return matchedHeader;
    }

    private String textValue(JsonNode node, String fieldName) {
        JsonNode valueNode = node.path(fieldName);

        if (valueNode.isMissingNode() || valueNode.isNull()) {
            return null;
        }

        return valueNode.asText();
    }
}

