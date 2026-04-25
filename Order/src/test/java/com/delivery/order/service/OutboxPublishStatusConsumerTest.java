package com.delivery.order.service;

import com.delivery.order.service.event.KafkaEventExtractor;
import com.delivery.order.service.outbox.OutboxPublishStatusConsumer;
import com.delivery.order.service.outbox.OutboxStatusService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxPublishStatusConsumerTest {
    @Mock
    private OutboxStatusService outboxStatusService;

    private OutboxPublishStatusConsumer outboxPublishStatusConsumer;

    @BeforeEach
    void setUp() {
        outboxPublishStatusConsumer = new OutboxPublishStatusConsumer(
            outboxStatusService,
            new KafkaEventExtractor(new ObjectMapper())
        );
    }

    @Test
    void Debezium_envelope_INIT_이벤트를_받으면_Outbox를_PUBLISHED로_자동_전환한다() {
        when(outboxStatusService.markPublishedIfInit("event-001")).thenReturn(true);

        ConsumerRecord<String, String> record = new ConsumerRecord<>(
            "delivery.delivery_order.outbox",
            0,
            0L,
            null,
            """
                {
                  "payload": {
                    "after": {
                      "event_id": "event-001",
                      "status": "INIT"
                    }
                  }
                }
                """
        );

        outboxPublishStatusConsumer.consume(record);

        verify(outboxStatusService).markPublishedIfInit("event-001");
    }

    @Test
    void SMT_헤더_id를_받으면_Outbox를_PUBLISHED로_자동_전환한다() {
        when(outboxStatusService.markPublishedIfInit("event-002")).thenReturn(true);

        ConsumerRecord<String, String> record = new ConsumerRecord<>(
            "outbox.event.ORDER",
            0,
            0L,
            "1",
            "{\"eventType\":\"OrderCreated\"}"
        );
        record.headers().add("id", "event-002".getBytes(StandardCharsets.UTF_8));

        outboxPublishStatusConsumer.consume(record);

        verify(outboxStatusService).markPublishedIfInit("event-002");
    }

    @Test
    void SMT_payload_wrapper에서_eventId를_찾아_PUBLISHED로_자동_전환한다() {
        when(outboxStatusService.markPublishedIfInit("event-003")).thenReturn(true);

        ConsumerRecord<String, String> record = new ConsumerRecord<>(
            "outbox.event.ORDER",
            0,
            0L,
            "1",
            """
                {
                  "schema": {
                    "type": "string"
                  },
                  "payload": "{\\"eventId\\":\\"event-003\\",\\"eventType\\":\\"OrderCreated\\"}"
                }
                """
        );

        outboxPublishStatusConsumer.consume(record);

        verify(outboxStatusService).markPublishedIfInit("event-003");
    }

    @Test
    void Debezium_PUBLISHED_이벤트를_받으면_추가_전환을_시도하지_않는다() {
        ConsumerRecord<String, String> record = new ConsumerRecord<>(
            "delivery.delivery_order.outbox",
            0,
            0L,
            null,
            """
                {
                  "payload": {
                    "after": {
                      "event_id": "event-004",
                      "status": "PUBLISHED"
                    }
                  }
                }
                """
        );

        outboxPublishStatusConsumer.consume(record);

        verify(outboxStatusService, never()).markPublishedIfInit("event-004");
    }
}
