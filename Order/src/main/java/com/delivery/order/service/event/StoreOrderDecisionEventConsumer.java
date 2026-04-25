package com.delivery.order.service.event;

import com.delivery.order.constant.OrderEventType;
import com.delivery.order.dto.event.StoreOrderDecisionEventDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

@Component
@RequiredArgsConstructor
@Slf4j
public class StoreOrderDecisionEventConsumer {
    private final KafkaEventExtractor kafkaEventExtractor;
    private final StoreOrderDecisionResultService storeOrderDecisionResultService;

    @KafkaListener(
        topics = {
            "${order.store-order-decision-event.order-accepted-topic:outbox.event.StoreOrderAccepted}",
            "${order.store-order-decision-event.order-rejected-topic:outbox.event.StoreOrderRejected}"
        },
        groupId = "${order.store-order-decision-event.consumer-group:order-store-order-decision}"
    )
    public void consume(ConsumerRecord<String, String> record) {
        try {
            StoreOrderDecisionEventDto event = extractEvent(record);
            if (event == null) {
                return;
            }

            if (!OrderEventType.STORE_ORDER_ACCEPTED.equals(event.eventType())
                && !OrderEventType.STORE_ORDER_REJECTED.equals(event.eventType())) {
                return;
            }

            storeOrderDecisionResultService.handle(event);
        } catch (Exception exception) {
            log.error(
                "Store order decision consume failed. topic={}, partition={}, offset={}",
                record.topic(),
                record.partition(),
                record.offset(),
                exception
            );
        }
    }

    private StoreOrderDecisionEventDto extractEvent(ConsumerRecord<String, String> record) throws Exception {
        KafkaEventExtractor.ExtractedEvent event = kafkaEventExtractor.extractEvent(record);
        if (event == null) {
            return null;
        }

        return toDto(event.payload(), event.eventType());
    }

    private StoreOrderDecisionEventDto toDto(JsonNode node, String eventType) {
        String eventId = kafkaEventExtractor.textValue(node, "eventId");
        Integer schemaVersion = kafkaEventExtractor.intValue(node, "schemaVersion");
        Long orderId = kafkaEventExtractor.longValue(node, "orderId");
        Long storeId = kafkaEventExtractor.longValue(node, "storeId");

        if (eventId == null || orderId == null || storeId == null) {
            return null;
        }
        if (schemaVersion == null) {
            schemaVersion = 1;
        }

        return new StoreOrderDecisionEventDto(
            eventId,
            eventType,
            schemaVersion,
            kafkaEventExtractor.localDateTimeValue(node, "occurredAt"),
            orderId,
            storeId,
            kafkaEventExtractor.textValue(node, "decision"),
            kafkaEventExtractor.textValue(node, "rejectReason")
        );
    }
}

