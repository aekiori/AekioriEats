package com.delivery.order.controller;

import com.delivery.order.domain.order.Order;
import com.delivery.order.dto.request.CreateOrderDto;
import com.delivery.order.dto.request.UpdateOrderStatusDto;
import com.delivery.order.dto.response.CreateOrderResultDto;
import com.delivery.order.dto.response.OrderDetailResultDto;
import com.delivery.order.dto.response.OrderPageResultDto;
import com.delivery.order.dto.response.UpdateOrderStatusResultDto;
import com.delivery.order.service.order.CreateOrderService;
import com.delivery.order.service.order.GetOrderService;
import com.delivery.order.service.order.GetOrdersService;
import com.delivery.order.service.order.UpdateOrderStatusService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/v1/orders")
public class OrderController {
    private final CreateOrderService createOrderService;
    private final GetOrderService getOrderService;
    private final GetOrdersService getOrdersService;
    private final UpdateOrderStatusService updateOrderStatusService;

    @PostMapping
    public ResponseEntity<CreateOrderResultDto> createOrder(
        @RequestHeader("X-Idempotency-Key") @NotBlank String idempotencyKey, // todo - 이거 걍 UUID 형식으로 강제시킬까
        @Valid @RequestBody CreateOrderDto createOrderDto
    ) {
        CreateOrderResultDto response = createOrderService.createOrder(createOrderDto, idempotencyKey);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderDetailResultDto> getOrder(@PathVariable Long orderId) {
        return ResponseEntity.ok(getOrderService.getOrder(orderId));
    }

    @GetMapping
    public ResponseEntity<OrderPageResultDto> getOrders(
        @RequestParam Long userId,
        @RequestParam(required = false) Order.Status status,
        @RequestParam(defaultValue = "0") @Min(0) int page,
        @RequestParam(defaultValue = "20") @Min(1) int size
    ) {
        return ResponseEntity.ok(getOrdersService.getOrders(userId, status, page, size));
    }

    @PatchMapping("/{orderId}/status")
    public ResponseEntity<UpdateOrderStatusResultDto> updateOrderStatus(
        @PathVariable Long orderId,
        @Valid @RequestBody UpdateOrderStatusDto updateOrderStatusDto
    ) {
        return ResponseEntity.ok(updateOrderStatusService.updateOrderStatus(orderId, updateOrderStatusDto));
    }
}
