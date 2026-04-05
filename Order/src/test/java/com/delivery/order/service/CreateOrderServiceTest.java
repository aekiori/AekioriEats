package com.delivery.order.service;

import com.delivery.order.dto.request.CreateOrderDto;
import com.delivery.order.dto.request.CreateOrderItemDto;
import com.delivery.order.exception.ApiException;
import com.delivery.order.repository.order.OrderItemRepository;
import com.delivery.order.repository.order.OrderRepository;
import com.delivery.order.service.order.CreateOrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateOrderServiceTest {
    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private OrderOutboxService orderOutboxService;

    @Mock
    private OrderIdempotencyCacheService orderIdempotencyCacheService;

    private CreateOrderService createOrderService;

    @BeforeEach
    void setUp() {
        createOrderService = new CreateOrderService(
            orderRepository,
            orderItemRepository,
            orderOutboxService,
            orderIdempotencyCacheService,
            new ObjectMapper()
        );
    }

    @Test
    void same_idempotency_key_with_different_request_throws_conflict() {
        CreateOrderDto request = createRequest("서울시 강남구 테헤란로 123");
        String requestHash = invokeHash(request);

        when(orderRepository.findByIdempotencyKey("idempotency-001")).thenReturn(Optional.empty());
        when(orderIdempotencyCacheService.getCompletedResult("idempotency-001")).thenReturn(null);
        when(orderIdempotencyCacheService.tryAcquire("idempotency-001", requestHash)).thenReturn(false);
        when(orderIdempotencyCacheService.getProcessingRequestHash("idempotency-001")).thenReturn("another-hash");

        assertThatThrownBy(() -> createOrderService.createOrder(request, "idempotency-001"))
            .isInstanceOf(ApiException.class)
            .satisfies(exception -> {
                ApiException apiException = (ApiException) exception;
                assertThat(apiException.getCode()).isEqualTo("IDEMPOTENCY_KEY_CONFLICT");
                assertThat(apiException.getStatus()).isEqualTo(HttpStatus.CONFLICT);
            })
            .hasMessageContaining("idempotencyKey");

        verifyNoInteractions(orderItemRepository, orderOutboxService);
    }

    @Test
    void same_idempotency_key_with_same_processing_request_throws_in_progress() {
        CreateOrderDto request = createRequest("서울시 강남구 테헤란로 123");
        String requestHash = invokeHash(request);

        when(orderRepository.findByIdempotencyKey("idempotency-002")).thenReturn(Optional.empty());
        when(orderIdempotencyCacheService.getCompletedResult("idempotency-002")).thenReturn(null);
        when(orderIdempotencyCacheService.tryAcquire("idempotency-002", requestHash)).thenReturn(false);
        when(orderIdempotencyCacheService.getProcessingRequestHash("idempotency-002")).thenReturn(requestHash);

        assertThatThrownBy(() -> createOrderService.createOrder(request, "idempotency-002"))
            .isInstanceOf(ApiException.class)
            .satisfies(exception -> {
                ApiException apiException = (ApiException) exception;
                assertThat(apiException.getCode()).isEqualTo("IDEMPOTENT_REQUEST_IN_PROGRESS");
                assertThat(apiException.getStatus()).isEqualTo(HttpStatus.CONFLICT);
            });

        verifyNoInteractions(orderItemRepository, orderOutboxService);
    }

    private String invokeHash(CreateOrderDto request) {
        try {
            java.lang.reflect.Method method = CreateOrderService.class.getDeclaredMethod("generateRequestHash", CreateOrderDto.class);
            method.setAccessible(true);
            return (String) method.invoke(createOrderService, request);
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }

    private CreateOrderDto createRequest(String deliveryAddress) {
        return new CreateOrderDto(
            1L,
            100L,
            deliveryAddress,
            1000,
            List.of(
                new CreateOrderItemDto(10L, "불고기버거", 8500, 2),
                new CreateOrderItemDto(20L, "콜라", 2000, 1)
            )
        );
    }
}
