package com.delivery.order.service.order;

import com.delivery.order.constant.OrderStatusChangeReason;
import com.delivery.order.domain.order.Order;
import com.delivery.order.domain.order.OrderStatusHistory;
import com.delivery.order.repository.order.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderTimeoutCompensationServiceTest {
    @Mock
    private OrderRepository orderRepository;

    @Mock
    private RecordOrderStatusHistoryService recordOrderStatusHistoryService;

    private OrderTimeoutCompensationService orderTimeoutCompensationService;

    @BeforeEach
    void setUp() {
        orderTimeoutCompensationService = new OrderTimeoutCompensationService(
            orderRepository,
            recordOrderStatusHistoryService
        );
        ReflectionTestUtils.setField(orderTimeoutCompensationService, "batchSize", 100);
        ReflectionTestUtils.setField(orderTimeoutCompensationService, "storeValidationTimeoutMinutes", 10L);
        ReflectionTestUtils.setField(orderTimeoutCompensationService, "paymentResultTimeoutMinutes", 10L);
        ReflectionTestUtils.setField(orderTimeoutCompensationService, "storeDecisionTimeoutMinutes", 15L);
    }

    @Test
    void pending_order_timeout_changes_status_to_failed() {
        Order order = createOrder(Order.Status.PENDING);

        when(orderRepository.findTimedOutOrdersForUpdate(eq(Order.Status.PENDING), any(LocalDateTime.class), any(Pageable.class)))
            .thenReturn(List.of(order));
        when(orderRepository.findTimedOutOrdersForUpdate(eq(Order.Status.PAYMENT_PENDING), any(LocalDateTime.class), any(Pageable.class)))
            .thenReturn(List.of());
        when(orderRepository.findTimedOutOrdersForUpdate(eq(Order.Status.PAID), any(LocalDateTime.class), any(Pageable.class)))
            .thenReturn(List.of());
        when(orderRepository.save(order)).thenReturn(order);

        int compensatedCount = orderTimeoutCompensationService.compensateTimedOutOrders();

        assertThat(compensatedCount).isEqualTo(1);
        assertThat(order.getStatus()).isEqualTo(Order.Status.FAILED);
        verify(recordOrderStatusHistoryService).record(
            order,
            Order.Status.PENDING,
            Order.Status.FAILED,
            OrderStatusChangeReason.STORE_VALIDATION_TIMEOUT,
            OrderStatusHistory.SourceType.TIMEOUT_COMPENSATION,
            null
        );
    }

    @Test
    void paid_order_timeout_changes_status_to_refund_pending() {
        Order order = createOrder(Order.Status.PAID);

        when(orderRepository.findTimedOutOrdersForUpdate(eq(Order.Status.PENDING), any(LocalDateTime.class), any(Pageable.class)))
            .thenReturn(List.of());
        when(orderRepository.findTimedOutOrdersForUpdate(eq(Order.Status.PAYMENT_PENDING), any(LocalDateTime.class), any(Pageable.class)))
            .thenReturn(List.of());
        when(orderRepository.findTimedOutOrdersForUpdate(eq(Order.Status.PAID), any(LocalDateTime.class), any(Pageable.class)))
            .thenReturn(List.of(order));
        when(orderRepository.save(order)).thenReturn(order);

        int compensatedCount = orderTimeoutCompensationService.compensateTimedOutOrders();

        assertThat(compensatedCount).isEqualTo(1);
        assertThat(order.getStatus()).isEqualTo(Order.Status.REFUND_PENDING);
        verify(recordOrderStatusHistoryService).record(
            order,
            Order.Status.PAID,
            Order.Status.REFUND_PENDING,
            OrderStatusChangeReason.STORE_DECISION_TIMEOUT,
            OrderStatusHistory.SourceType.TIMEOUT_COMPENSATION,
            null
        );
    }

    private Order createOrder(Order.Status status) {
        return new Order(
            1L,
            100L,
            status,
            "Seoul Gangnam",
            10000,
            0,
            10000,
            "idempotency-key",
            "request-hash"
        );
    }
}
