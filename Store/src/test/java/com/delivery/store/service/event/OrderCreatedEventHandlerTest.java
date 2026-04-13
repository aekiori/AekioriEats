package com.delivery.store.service.event;

import com.delivery.store.domain.outbox.Outbox;
import com.delivery.store.domain.store.StoreOrderValidation;
import com.delivery.store.dto.event.OrderCreatedEventDto;
import com.delivery.store.repository.outbox.OutboxRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderCreatedEventHandlerTest {
    @Mock
    private StoreOrderableValidator storeOrderableValidator;

    @Mock
    private StoreOrderValidationHistoryService storeOrderValidationHistoryService;

    @Mock
    private OutboxRepository outboxRepository;

    private OrderCreatedEventHandler orderCreatedEventHandler;

    @BeforeEach
    void setUp() {
        orderCreatedEventHandler = new OrderCreatedEventHandler(
            storeOrderableValidator,
            storeOrderValidationHistoryService,
            outboxRepository
        );
        when(storeOrderValidationHistoryService.save(any(), any())).thenReturn(
            StoreOrderValidation.createAccepted(1L, 101L, LocalDateTime.now())
        );
    }

    @Test
    void handle_saves_validated_outbox_when_validation_passes() {
        OrderCreatedEventDto event = createEvent(1L, 101L);
        when(storeOrderableValidator.validate(event)).thenReturn(StoreOrderValidationResult.pass());

        orderCreatedEventHandler.handle(event);

        verify(storeOrderValidationHistoryService).save(event, StoreOrderValidationResult.pass());
        ArgumentCaptor<Outbox> captor = ArgumentCaptor.forClass(Outbox.class);
        verify(outboxRepository).save(captor.capture());
        assertThat(captor.getValue().getEventType()).isEqualTo("OrderValidated");
        assertThat(captor.getValue().getAggregateId()).isEqualTo(1L);
    }

    @Test
    void handle_saves_rejected_outbox_when_validation_fails() {
        OrderCreatedEventDto event = createEvent(2L, 102L);
        StoreOrderValidationResult rejected = StoreOrderValidationResult.reject(
            "MIN_ORDER_AMOUNT_NOT_MET",
            "totalAmount=10000, minOrderAmount=15000"
        );
        when(storeOrderableValidator.validate(event)).thenReturn(rejected);

        orderCreatedEventHandler.handle(event);

        verify(storeOrderValidationHistoryService).save(event, rejected);
        ArgumentCaptor<Outbox> captor = ArgumentCaptor.forClass(Outbox.class);
        verify(outboxRepository).save(captor.capture());
        assertThat(captor.getValue().getEventType()).isEqualTo("OrderRejected");
        assertThat(captor.getValue().getAggregateId()).isEqualTo(2L);
    }

    private OrderCreatedEventDto createEvent(Long orderId, Long storeId) {
        return new OrderCreatedEventDto(
            "event-" + orderId,
            "OrderCreated",
            1,
            LocalDateTime.parse("2026-04-13T22:50:00"),
            orderId,
            10L,
            storeId,
            20000,
            0,
            20000,
            "PENDING"
        );
    }
}
