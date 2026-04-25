package com.delivery.order.service.event;

import com.delivery.order.constant.OrderEventType;
import com.delivery.order.dto.event.StoreOrderValidationEventDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

@Component
@RequiredArgsConstructor
@Slf4j
public class StoreOrderValidationEventConsumer {
    private final KafkaEventExtractor kafkaEventExtractor;
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
            throw new RuntimeException(exception);
        }
    }

    private StoreOrderValidationEventDto extractEvent(ConsumerRecord<String, String> record) throws Exception {
        KafkaEventExtractor.ExtractedEvent event = kafkaEventExtractor.extractEvent(record);
        if (event == null) {
            return null;
        }

        return toDto(event.payload(), event.eventType());
    }

    private StoreOrderValidationEventDto toDto(JsonNode node, String eventType) {
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

        return new StoreOrderValidationEventDto(
            eventId,
            eventType,
            schemaVersion,
            kafkaEventExtractor.localDateTimeValue(node, "occurredAt"),
            orderId,
            storeId,
            kafkaEventExtractor.textValue(node, "validationResult"),
            kafkaEventExtractor.textValue(node, "rejectCode"),
            kafkaEventExtractor.textValue(node, "rejectReason")
        );
    }
}

