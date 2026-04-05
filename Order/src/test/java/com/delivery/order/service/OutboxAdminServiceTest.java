package com.delivery.order.service;

import com.delivery.order.domain.outbox.Outbox;
import com.delivery.order.dto.response.OutboxReplayResultDto;
import com.delivery.order.exception.ApiException;
import com.delivery.order.repository.outbox.OutboxRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.messaging.Message;

import java.lang.reflect.Field;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxAdminServiceTest {
    @Mock
    private OutboxRepository outboxRepository;

    @Mock
    private OutboxStatusService outboxStatusService;

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    private OutboxAdminService outboxAdminService;

    @BeforeEach
    void setUp() throws Exception {
        outboxAdminService = new OutboxAdminService(
            outboxRepository,
            outboxStatusService,
            kafkaTemplate
        );

        Field field = OutboxAdminService.class.getDeclaredField("outboxSmtTopic");
        field.setAccessible(true);
        field.set(outboxAdminService, "outbox.event.ORDER");
    }

    @Test
    void FAILED_Outbox는_수동_재처리_발행할_수_있다() {
        Outbox outbox = new Outbox(
            "event-001",
            "ORDER",
            10L,
            "OrderCreated",
            "{\"eventId\":\"event-001\"}",
            Outbox.Status.FAILED,
            "1"
        );

        when(outboxRepository.findByEventId("event-001")).thenReturn(Optional.of(outbox));
        doReturn((CompletableFuture<SendResult<String, String>>) (CompletableFuture<?>) CompletableFuture.completedFuture(mock(SendResult.class)))
            .when(kafkaTemplate)
            .send(any(Message.class));

        OutboxReplayResultDto result = outboxAdminService.replayFailedOutbox("event-001");

        verify(outboxStatusService).resetToInit("event-001");
        assertThat(result.eventId()).isEqualTo("event-001");
        assertThat(result.status()).isEqualTo("INIT");
        assertThat(result.topic()).isEqualTo("outbox.event.ORDER");
    }

    @Test
    void FAILED가_아닌_Outbox는_수동_재처리할_수_없다() {
        Outbox outbox = new Outbox(
            "event-002",
            "ORDER",
            11L,
            "OrderCreated",
            "{\"eventId\":\"event-002\"}",
            Outbox.Status.PUBLISHED,
            "1"
        );

        when(outboxRepository.findByEventId("event-002")).thenReturn(Optional.of(outbox));

        assertThatThrownBy(() -> outboxAdminService.replayFailedOutbox("event-002"))
            .isInstanceOf(ApiException.class)
            .hasMessageContaining("FAILED 상태의 Outbox만 재처리할 수 있다.");

        verify(outboxStatusService, never()).resetToInit("event-002");
    }
}
