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
import com.delivery.order.service.order.OrderAuthorizationService;
import com.delivery.order.service.order.UpdateOrderStatusService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
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
    private final OrderAuthorizationService orderAuthorizationService;

    @PostMapping
    public ResponseEntity<CreateOrderResultDto> createOrder(
        @RequestHeader(value = "X-User-Id", required = false)
        String authenticatedUserIdHeader,
        @RequestHeader(value = "X-User-Role", required = false)
        String authenticatedUserRole,
        @RequestHeader("X-Idempotency-Key")
        @NotBlank
        @Pattern(regexp = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-4[0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}$")
        String idempotencyKey,
        @RequestBody @Valid CreateOrderDto createOrderDto
    ) {
        long authenticatedUserId = orderAuthorizationService.parseAuthenticatedUserId(authenticatedUserIdHeader);
        CreateOrderResultDto response = createOrderService.createOrder(
            createOrderDto,
            idempotencyKey,
            authenticatedUserId,
            authenticatedUserRole
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderDetailResultDto> getOrder(
        @PathVariable Long orderId,
        @RequestHeader(value = "X-User-Id", required = false)
        String authenticatedUserIdHeader,
        @RequestHeader(value = "X-User-Role", required = false)
        String authenticatedUserRole
    ) {
        long authenticatedUserId = orderAuthorizationService.parseAuthenticatedUserId(authenticatedUserIdHeader);
        return ResponseEntity.ok(getOrderService.getOrder(orderId, authenticatedUserId, authenticatedUserRole));
    }

    @GetMapping
    public ResponseEntity<OrderPageResultDto> getOrders(
        @RequestParam Long userId,
        @RequestParam(required = false) Order.Status status,
        @RequestParam(defaultValue = "0") @Min(0) int page,
        @RequestParam(defaultValue = "20") @Min(1) int limit,
        @RequestHeader(value = "X-User-Id", required = false)
        String authenticatedUserIdHeader,
        @RequestHeader(value = "X-User-Role", required = false)
        String authenticatedUserRole
    ) {
        long authenticatedUserId = orderAuthorizationService.parseAuthenticatedUserId(authenticatedUserIdHeader);
        return ResponseEntity.ok(
            getOrdersService.getOrders(userId, status, page, limit, authenticatedUserId, authenticatedUserRole)
        );
    }

    @PatchMapping("/{orderId}/status")
    public ResponseEntity<UpdateOrderStatusResultDto> updateOrderStatus(
        @PathVariable Long orderId,
        @Valid @RequestBody UpdateOrderStatusDto updateOrderStatusDto,
        @RequestHeader(value = "X-User-Id", required = false)
        String authenticatedUserIdHeader,
        @RequestHeader(value = "X-User-Role", required = false)
        String authenticatedUserRole
    ) {
        long authenticatedUserId = orderAuthorizationService.parseAuthenticatedUserId(authenticatedUserIdHeader);
        return ResponseEntity.ok(
            updateOrderStatusService.updateOrderStatus(
                orderId,
                updateOrderStatusDto,
                authenticatedUserId,
                authenticatedUserRole
            )
        );
    }
}
