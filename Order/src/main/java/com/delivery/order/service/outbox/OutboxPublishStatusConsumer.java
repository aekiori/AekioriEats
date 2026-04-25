package com.delivery.order.service.outbox;

import com.delivery.order.service.event.KafkaEventExtractor;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OutboxPublishStatusConsumer {
    private static final Logger log = LoggerFactory.getLogger(OutboxPublishStatusConsumer.class);

    private final OutboxStatusService outboxStatusService;
    private final KafkaEventExtractor kafkaEventExtractor;

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
            throw new RuntimeException(exception);
        }
    }

    private String extractEventId(ConsumerRecord<String, String> record) throws Exception {
        return kafkaEventExtractor.extractOutboxEventId(record);
    }
}

