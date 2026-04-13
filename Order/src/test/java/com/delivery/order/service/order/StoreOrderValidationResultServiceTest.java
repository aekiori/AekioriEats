package com.delivery.order.service.order;

import com.delivery.order.constant.OrderEventType;
import com.delivery.order.domain.order.Order;
import com.delivery.order.dto.event.StoreOrderValidationEventDto;
import com.delivery.order.repository.order.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StoreOrderValidationResultServiceTest {
    @Mock
    private OrderRepository orderRepository;

    private StoreOrderValidationResultService storeOrderValidationResultService;

    @BeforeEach
    void setUp() {
        storeOrderValidationResultService = new StoreOrderValidationResultService(orderRepository);
    }

    @Test
    void validated_event_changes_status_to_payment_pending() {
        Order order = createPendingOrder(101L);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        storeOrderValidationResultService.handle(
            new StoreOrderValidationEventDto(
                "event-validated-1",
                OrderEventType.ORDER_VALIDATED,
                1,
                LocalDateTime.now(),
                1L,
                101L,
                "ACCEPTED",
                null,
                null
            )
        );

        assertThat(order.getStatus()).isEqualTo(Order.Status.PAYMENT_PENDING);
        verify(orderRepository).save(order);
    }

    @Test
    void rejected_event_changes_status_to_failed() {
        Order order = createPendingOrder(102L);
        when(orderRepository.findById(2L)).thenReturn(Optional.of(order));

        storeOrderValidationResultService.handle(
            new StoreOrderValidationEventDto(
                "event-rejected-2",
                OrderEventType.ORDER_REJECTED,
                1,
                LocalDateTime.now(),
                2L,
                102L,
                "REJECTED",
                "MIN_ORDER_AMOUNT_NOT_MET",
                "totalAmount=12000, minOrderAmount=15000"
            )
        );

        assertThat(order.getStatus()).isEqualTo(Order.Status.FAILED);
        verify(orderRepository).save(order);
    }

    @Test
    void event_is_ignored_when_store_id_does_not_match() {
        Order order = createPendingOrder(103L);
        when(orderRepository.findById(3L)).thenReturn(Optional.of(order));

        storeOrderValidationResultService.handle(
            new StoreOrderValidationEventDto(
                "event-validated-3",
                OrderEventType.ORDER_VALIDATED,
                1,
                LocalDateTime.now(),
                3L,
                999L,
                "ACCEPTED",
                null,
                null
            )
        );

        assertThat(order.getStatus()).isEqualTo(Order.Status.PENDING);
        verify(orderRepository, never()).save(order);
    }

    private Order createPendingOrder(Long storeId) {
        return new Order(
            1L,
            storeId,
            Order.Status.PENDING,
            "Seoul",
            20000,
            0,
            20000,
            "idem-" + storeId,
            "hash-" + storeId
        );
    }
}
