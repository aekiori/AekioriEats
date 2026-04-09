package com.delivery.order.service;

import com.delivery.order.TestIdempotencyCacheConfig;
import com.delivery.order.domain.order.Order;
import com.delivery.order.domain.order.exception.InvalidOrderStatusTransitionException;
import com.delivery.order.domain.outbox.Outbox;
import com.delivery.order.dto.request.CreateOrderDto;
import com.delivery.order.dto.request.CreateOrderItemDto;
import com.delivery.order.dto.request.UpdateOrderStatusDto;
import com.delivery.order.dto.response.CreateOrderResultDto;
import com.delivery.order.dto.response.OrderDetailResultDto;
import com.delivery.order.dto.response.OrderPageResultDto;
import com.delivery.order.dto.response.UpdateOrderStatusResultDto;
import com.delivery.order.exception.ApiException;
import com.delivery.order.repository.order.OrderItemRepository;
import com.delivery.order.repository.order.OrderRepository;
import com.delivery.order.repository.outbox.OutboxRepository;
import com.delivery.order.service.order.CreateOrderService;
import com.delivery.order.service.order.GetOrderService;
import com.delivery.order.service.order.GetOrdersService;
import com.delivery.order.service.order.UpdateOrderStatusService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestIdempotencyCacheConfig.class)
class OrderServiceIntegrationTest {
    @Autowired
    private CreateOrderService createOrderService;

    @Autowired
    private GetOrderService getOrderService;

    @Autowired
    private GetOrdersService getOrdersService;

    @Autowired
    private UpdateOrderStatusService updateOrderStatusService;

    @Autowired
    private OutboxStatusService outboxStatusService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private OutboxRepository outboxRepository;

    @AfterEach
    void tearDown() {
        outboxRepository.deleteAll();
        orderItemRepository.deleteAll();
        orderRepository.deleteAll();
    }

    @Test
    void create_order_persists_order_items_and_order_created_outbox() {
        CreateOrderDto request = new CreateOrderDto(
            1L,
            100L,
            "서울시 강남구 테헤란로 123",
            1000,
            List.of(
                new CreateOrderItemDto(10L, "불고기버거", 8500, 2),
                new CreateOrderItemDto(20L, "콜라", 2000, 1)
            )
        );

        CreateOrderResultDto result = createOrderService.createOrder(request, "order-create-integration-001");

        assertThat(result.orderId()).isNotNull();
        assertThat(result.status()).isEqualTo("PENDING");
        assertThat(result.totalAmount()).isEqualTo(19000);
        assertThat(result.finalAmount()).isEqualTo(18000);

        Order savedOrder = orderRepository.findById(result.orderId()).orElseThrow();

        assertThat(savedOrder.getUserId()).isEqualTo(1L);
        assertThat(savedOrder.getStoreId()).isEqualTo(100L);
        assertThat(savedOrder.getStatus()).isEqualTo(Order.Status.PENDING);

        assertThat(orderItemRepository.findByOrderId(savedOrder.getId()))
            .hasSize(2)
            .extracting("menuName")
            .containsExactly("불고기버거", "콜라");

        assertThat(outboxRepository.findAll())
            .hasSize(1)
            .first()
            .satisfies(outbox -> {
                assertThat(outbox.getAggregateType()).isEqualTo(Outbox.AggregateType.ORDER);
                assertThat(outbox.getAggregateId()).isEqualTo(savedOrder.getId());
                assertThat(outbox.getEventType()).isEqualTo("OrderCreated");
                assertThat(outbox.getStatus()).isEqualTo(Outbox.Status.INIT);
                assertThat(outbox.getPartitionKey()).isEqualTo("1");
                assertThat(outbox.getPayload()).contains("OrderCreated");
                assertThat(outbox.getPayload()).contains("orderId");
                assertThat(outbox.getPayload()).contains(String.valueOf(savedOrder.getId()));
            });
    }

    @Test
    void get_order_returns_order_detail_and_items() {
        Long orderId = createPendingOrder();

        OrderDetailResultDto result = getOrderService.getOrder(orderId);

        assertThat(result.orderId()).isEqualTo(orderId);
        assertThat(result.userId()).isEqualTo(1L);
        assertThat(result.storeId()).isEqualTo(100L);
        assertThat(result.status()).isEqualTo("PENDING");
        assertThat(result.items()).hasSize(2);
        assertThat(result.items())
            .extracting("menuName")
            .containsExactly("불고기버거", "콜라");
    }

    @Test
    void get_orders_filters_by_user_and_status() {
        Long firstOrderId = createPendingOrder();
        Long secondOrderId = createPendingOrder(1L, 101L, 0);
        createPendingOrder(2L, 200L, 0);

        updateOrderStatusService.updateOrderStatus(
            secondOrderId,
            new UpdateOrderStatusDto(Order.Status.PAID, "payment completed")
        );

        OrderPageResultDto result = getOrdersService.getOrders(1L, Order.Status.PENDING, 0, 20);

        assertThat(result.content()).hasSize(1);
        assertThat(result.content().get(0).orderId()).isEqualTo(firstOrderId);
        assertThat(result.content().get(0).status()).isEqualTo("PENDING");
        assertThat(result.totalElements()).isEqualTo(1);
    }

    @Test
    void update_order_status_persists_order_status_changed_outbox() {
        Long orderId = createPendingOrder();

        UpdateOrderStatusResultDto result = updateOrderStatusService.updateOrderStatus(
            orderId,
            new UpdateOrderStatusDto(Order.Status.PAID, "payment completed")
        );

        Order updatedOrder = orderRepository.findById(orderId).orElseThrow();

        assertThat(result.orderId()).isEqualTo(orderId);
        assertThat(result.status()).isEqualTo("PAID");
        assertThat(updatedOrder.getStatus()).isEqualTo(Order.Status.PAID);

        assertThat(outboxRepository.findAll()).hasSize(2);

        assertThat(outboxRepository.findAll().stream()
            .filter(outbox -> outbox.getEventType().equals("OrderStatusChanged"))
            .findFirst())
            .isPresent()
            .get()
            .satisfies(outbox -> {
                assertThat(outbox.getAggregateId()).isEqualTo(orderId);
                assertThat(outbox.getStatus()).isEqualTo(Outbox.Status.INIT);
                assertThat(outbox.getPayload()).contains("OrderStatusChanged");
                assertThat(outbox.getPayload()).contains("currentStatus");
                assertThat(outbox.getPayload()).contains("PENDING");
                assertThat(outbox.getPayload()).contains("targetStatus");
                assertThat(outbox.getPayload()).contains("PAID");
                assertThat(outbox.getPayload()).contains("payment completed");
            });
    }

    @Test
    void invalid_order_status_transition_does_not_create_new_outbox() {
        Long orderId = createPendingOrder();
        updateOrderStatusService.updateOrderStatus(
            orderId,
            new UpdateOrderStatusDto(Order.Status.PAID, "payment completed")
        );

        assertThatThrownBy(() -> updateOrderStatusService.updateOrderStatus(
            orderId,
            new UpdateOrderStatusDto(Order.Status.FAILED, "invalid transition")
        ))
            .isInstanceOf(InvalidOrderStatusTransitionException.class)
            .hasMessageContaining("currentStatus=PAID")
            .hasMessageContaining("targetStatus=FAILED");

        Order order = orderRepository.findById(orderId).orElseThrow();

        assertThat(order.getStatus()).isEqualTo(Order.Status.PAID);
        assertThat(outboxRepository.findAll()).hasSize(2);
    }

    @Test
    void invalid_amount_rolls_back_all_persistence() {
        CreateOrderDto request = new CreateOrderDto(
            1L,
            100L,
            "서울시 강남구 테헤란로 123",
            20000,
            List.of(
                new CreateOrderItemDto(10L, "불고기버거", 8500, 2),
                new CreateOrderItemDto(20L, "콜라", 2000, 1)
            )
        );

        assertThatThrownBy(() -> createOrderService.createOrder(request, "order-create-invalid-amount-001"))
            .isInstanceOf(ApiException.class)
            .hasMessageContaining("0");

        assertThat(orderRepository.findAll()).isEmpty();
        assertThat(orderItemRepository.findAll()).isEmpty();
        assertThat(outboxRepository.findAll()).isEmpty();
    }

    @Test
    void same_idempotency_key_returns_existing_order_without_duplicate_outbox() {
        CreateOrderDto request = new CreateOrderDto(
            1L,
            100L,
            "서울시 강남구 테헤란로 123",
            1000,
            List.of(
                new CreateOrderItemDto(10L, "불고기버거", 8500, 2),
                new CreateOrderItemDto(20L, "콜라", 2000, 1)
            )
        );

        CreateOrderResultDto firstResult = createOrderService.createOrder(request, "order-create-001");
        CreateOrderResultDto secondResult = createOrderService.createOrder(request, "order-create-001");

        assertThat(secondResult.orderId()).isEqualTo(firstResult.orderId());
        assertThat(orderRepository.findAll()).hasSize(1);
        assertThat(orderItemRepository.findAll()).hasSize(2);
        assertThat(outboxRepository.findAll()).hasSize(1);
        assertThat(orderRepository.findById(firstResult.orderId()).orElseThrow().getIdempotencyKey())
            .isEqualTo("order-create-001");
    }

    @Test
    void conflicting_request_with_same_idempotency_key_throws_conflict() {
        createOrderService.createOrder(
            new CreateOrderDto(
                1L,
                100L,
                "서울시 강남구 테헤란로 123",
                1000,
                List.of(
                    new CreateOrderItemDto(10L, "불고기버거", 8500, 2)
                )
            ),
            "order-create-conflict-001"
        );

        assertThatThrownBy(() -> createOrderService.createOrder(
            new CreateOrderDto(
                1L,
                100L,
                "서울시 강남구 테헤란로 999",
                1000,
                List.of(
                    new CreateOrderItemDto(10L, "불고기버거", 8500, 2)
                )
            ),
            "order-create-conflict-001"
        ))
            .isInstanceOf(ApiException.class)
            .hasMessageContaining("idempotencyKey");
    }

    @Test
    void outbox_status_can_be_changed_to_published_and_failed() {
        Long orderId = createPendingOrder();

        Outbox createdOutbox = outboxRepository.findAll().get(0);

        outboxStatusService.markPublished(createdOutbox.getEventId());
        assertThat(outboxRepository.findById(createdOutbox.getId()).orElseThrow().getStatus())
            .isEqualTo(Outbox.Status.PUBLISHED);

        updateOrderStatusService.updateOrderStatus(
            orderId,
            new UpdateOrderStatusDto(Order.Status.PAID, "payment completed")
        );

        Outbox statusChangedOutbox = outboxRepository.findAll().stream()
            .filter(outbox -> outbox.getEventType().equals("OrderStatusChanged"))
            .findFirst()
            .orElseThrow();

        outboxStatusService.markFailed(statusChangedOutbox.getEventId());
        assertThat(outboxRepository.findById(statusChangedOutbox.getId()).orElseThrow().getStatus())
            .isEqualTo(Outbox.Status.FAILED);
    }

    private Long createPendingOrder() {
        return createPendingOrder(1L, 100L, 1000);
    }

    private Long createPendingOrder(Long userId, Long storeId, int usedPointAmount) {
        CreateOrderResultDto result = createOrderService.createOrder(
            new CreateOrderDto(
                userId,
                storeId,
                "서울시 강남구 테헤란로 123",
                usedPointAmount,
                List.of(
                    new CreateOrderItemDto(10L, "불고기버거", 8500, 2),
                    new CreateOrderItemDto(20L, "콜라", 2000, 1)
                )
            ),
            "order-create-pending-" + userId + "-" + storeId + "-" + usedPointAmount
        );

        return result.orderId();
    }
}
