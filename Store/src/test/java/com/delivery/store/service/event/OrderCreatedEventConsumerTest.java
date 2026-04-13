package com.delivery.store.service.event;

import com.delivery.store.dto.event.OrderCreatedEventDto;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class OrderCreatedEventConsumerTest {
    @Mock
    private OrderCreatedEventHandler orderCreatedEventHandler;

    private OrderCreatedEventConsumer orderCreatedEventConsumer;

    @BeforeEach
    void setUp() {
        orderCreatedEventConsumer = new OrderCreatedEventConsumer(
            new ObjectMapper(),
            orderCreatedEventHandler
        );
    }

    @Test
    void consume_smt_direct_payload_calls_validation_service() {
        ConsumerRecord<String, String> record = new ConsumerRecord<>(
            "outbox.event.ORDER",
            0,
            0L,
            "1",
            """
                {
                  "eventId": "event-001",
                  "eventType": "OrderCreated",
                  "schemaVersion": 1,
                  "occurredAt": "2026-04-12T10:00:00",
                  "orderId": 1,
                  "userId": 11,
                  "storeId": 101,
                  "totalAmount": 20000,
                  "usedPointAmount": 0,
                  "finalAmount": 20000,
                  "status": "PENDING"
                }
                """
        );

        orderCreatedEventConsumer.consume(record);

        ArgumentCaptor<OrderCreatedEventDto> captor = ArgumentCaptor.forClass(OrderCreatedEventDto.class);
        verify(orderCreatedEventHandler).handle(captor.capture());
        assertThat(captor.getValue().eventId()).isEqualTo("event-001");
        assertThat(captor.getValue().storeId()).isEqualTo(101L);
        assertThat(captor.getValue().totalAmount()).isEqualTo(20000);
    }

    @Test
    void consume_debezium_envelope_payload_calls_validation_service() {
        ConsumerRecord<String, String> record = new ConsumerRecord<>(
            "delivery.delivery_order.outbox",
            0,
            0L,
            null,
            """
                {
                  "payload": {
                    "after": {
                      "event_type": "OrderCreated",
                      "payload": "{\\"eventId\\":\\"event-002\\",\\"eventType\\":\\"OrderCreated\\",\\"orderId\\":2,\\"userId\\":12,\\"storeId\\":102,\\"totalAmount\\":21000,\\"usedPointAmount\\":0,\\"finalAmount\\":21000,\\"status\\":\\"PENDING\\",\\"schemaVersion\\":1,\\"occurredAt\\":\\"2026-04-12T10:01:00\\"}"
                    }
                  }
                }
                """
        );

        orderCreatedEventConsumer.consume(record);

        ArgumentCaptor<OrderCreatedEventDto> captor = ArgumentCaptor.forClass(OrderCreatedEventDto.class);
        verify(orderCreatedEventHandler).handle(captor.capture());
        assertThat(captor.getValue().eventId()).isEqualTo("event-002");
        assertThat(captor.getValue().storeId()).isEqualTo(102L);
    }

    @Test
    void consume_skips_non_order_created_event() {
        ConsumerRecord<String, String> record = new ConsumerRecord<>(
            "outbox.event.ORDER",
            0,
            0L,
            "1",
            """
                {
                  "eventId": "event-003",
                  "eventType": "OrderStatusChanged",
                  "orderId": 3,
                  "storeId": 103,
                  "totalAmount": 22000
                }
                """
        );

        orderCreatedEventConsumer.consume(record);

        verifyNoInteractions(orderCreatedEventHandler);
    }

    @Test
    void consume_uses_header_event_type_when_payload_event_type_is_missing() {
        ConsumerRecord<String, String> record = new ConsumerRecord<>(
            "outbox.event.ORDER",
            0,
            0L,
            "1",
            """
                {
                  "payload": "{\\"eventId\\":\\"event-004\\",\\"orderId\\":4,\\"userId\\":13,\\"storeId\\":104,\\"totalAmount\\":23000,\\"usedPointAmount\\":0,\\"finalAmount\\":23000,\\"status\\":\\"PENDING\\",\\"schemaVersion\\":1,\\"occurredAt\\":\\"2026-04-12T10:02:00\\"}"
                }
                """
        );
        record.headers().add("eventType", "OrderCreated".getBytes(StandardCharsets.UTF_8));

        orderCreatedEventConsumer.consume(record);

        ArgumentCaptor<OrderCreatedEventDto> captor = ArgumentCaptor.forClass(OrderCreatedEventDto.class);
        verify(orderCreatedEventHandler).handle(captor.capture());
        assertThat(captor.getValue().eventId()).isEqualTo("event-004");
        assertThat(captor.getValue().storeId()).isEqualTo(104L);
    }
}
