package com.delivery.order.controller.admin;

import com.delivery.order.domain.order.Order;
import com.delivery.order.dto.response.OrderPageResultDto;
import com.delivery.order.service.order.GetOrdersService;
import com.delivery.order.service.order.OrderAuthorizationService;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/orders")
public class AdminOrderController {
    private final GetOrdersService getOrdersService;
    private final OrderAuthorizationService orderAuthorizationService;

    @GetMapping
    public ResponseEntity<OrderPageResultDto> getOrdersByUser(
        @RequestParam Long userId,
        @RequestParam(required = false) Order.Status status,
        @RequestParam(defaultValue = "0") @Min(0) int page,
        @RequestParam(defaultValue = "20") @Min(1) int limit,
        @RequestHeader(value = "X-User-Id", required = true)
        String authenticatedUserIdHeader,
        @RequestHeader(value = "X-User-Role", required = true)
        String authenticatedUserRole
    ) {
        orderAuthorizationService.parseAuthenticatedUserId(authenticatedUserIdHeader);
        orderAuthorizationService.requireAdmin(authenticatedUserRole);
        return ResponseEntity.ok(getOrdersService.getOrders(userId, status, page, limit));
    }
}
