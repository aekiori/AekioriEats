package com.delivery.order.service.event;

import com.delivery.order.constant.OrderEventType;
import com.delivery.order.dto.event.PaymentResultEventDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentResultEventConsumer {
    private final KafkaEventExtractor kafkaEventExtractor;
    private final PaymentResultService paymentResultService;

    @KafkaListener(
        topics = {
            "${order.payment-result-event.payment-succeeded-topic:outbox.event.PaymentSucceeded}",
            "${order.payment-result-event.payment-failed-topic:outbox.event.PaymentFailed}",
            "${order.payment-result-event.payment-refunded-topic:outbox.event.PaymentRefunded}"
        },
        groupId = "${order.payment-result-event.consumer-group:order-payment-result}"
    )
    public void consume(ConsumerRecord<String, String> record) {
        try {
            PaymentResultEventDto event = extractEvent(record);

            if (event == null) {
                return;
            }

            if (!OrderEventType.PAYMENT_SUCCEEDED.equals(event.eventType())
                && !OrderEventType.PAYMENT_FAILED.equals(event.eventType())
                && !OrderEventType.PAYMENT_REFUNDED.equals(event.eventType())) {
                return;
            }

            paymentResultService.handle(event);
        } catch (Exception exception) {
            log.error(
                "Payment result consume failed. topic={}, partition={}, offset={}",
                record.topic(),
                record.partition(),
                record.offset(),
                exception
            );
            throw new RuntimeException(exception);
        }
    }

    private PaymentResultEventDto extractEvent(ConsumerRecord<String, String> record) throws Exception {
        KafkaEventExtractor.ExtractedEvent event = kafkaEventExtractor.extractEvent(record);
        if (event == null) {
            return null;
        }

        return toDto(event.payload(), event.eventType());
    }

    private PaymentResultEventDto toDto(JsonNode node, String eventType) {
        String eventId = kafkaEventExtractor.textValue(node, "eventId");
        Integer schemaVersion = kafkaEventExtractor.intValue(node, "schemaVersion");
        Long orderId = kafkaEventExtractor.longValue(node, "orderId");
        Long paymentId = kafkaEventExtractor.longValue(node, "paymentId");

        if (eventId == null || orderId == null || paymentId == null) {
            return null;
        }

        if (schemaVersion == null) {
            schemaVersion = 1;
        }

        return new PaymentResultEventDto(
            eventId,
            eventType,
            schemaVersion,
            kafkaEventExtractor.localDateTimeValue(node, "occurredAt"),
            orderId,
            paymentId,
            kafkaEventExtractor.textValue(node, "paymentStatus"),
            kafkaEventExtractor.intValue(node, "finalAmount"),
            kafkaEventExtractor.intValue(node, "usedPointAmount"),
            kafkaEventExtractor.textValue(node, "failReason")
        );
    }
}

