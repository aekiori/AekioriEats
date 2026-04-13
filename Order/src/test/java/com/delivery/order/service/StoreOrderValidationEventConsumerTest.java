package com.delivery.order.service;

import com.delivery.order.service.order.StoreOrderValidationResultService;
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
class StoreOrderValidationEventConsumerTest {
    @Mock
    private StoreOrderValidationResultService storeOrderValidationResultService;

    private StoreOrderValidationEventConsumer storeOrderValidationEventConsumer;

    @BeforeEach
    void setUp() {
        storeOrderValidationEventConsumer = new StoreOrderValidationEventConsumer(
            new ObjectMapper(),
            storeOrderValidationResultService
        );
    }

    @Test
    void consumes_order_validated_event_from_smt_payload() {
        ConsumerRecord<String, String> record = new ConsumerRecord<>(
            "outbox.event.ORDER",
            0,
            0L,
            "1",
            """
                {
                  "eventId": "event-validated-1",
                  "orderId": 1,
                  "storeId": 101,
                  "validationResult": "ACCEPTED"
                }
                """
        );
        record.headers().add("eventType", "OrderValidated".getBytes(StandardCharsets.UTF_8));

        storeOrderValidationEventConsumer.consume(record);

        ArgumentCaptor<com.delivery.order.dto.event.StoreOrderValidationEventDto> captor = ArgumentCaptor.forClass(
            com.delivery.order.dto.event.StoreOrderValidationEventDto.class
        );
        verify(storeOrderValidationResultService).handle(captor.capture());
        assertThat(captor.getValue().eventType()).isEqualTo("OrderValidated");
        assertThat(captor.getValue().orderId()).isEqualTo(1L);
        assertThat(captor.getValue().storeId()).isEqualTo(101L);
    }

    @Test
    void skips_non_target_event_type() {
        ConsumerRecord<String, String> record = new ConsumerRecord<>(
            "outbox.event.ORDER",
            0,
            0L,
            "1",
            """
                {
                  "eventId": "event-created-1",
                  "eventType": "OrderCreated",
                  "orderId": 1,
                  "storeId": 101
                }
                """
        );

        storeOrderValidationEventConsumer.consume(record);

        verifyNoInteractions(storeOrderValidationResultService);
    }
}
