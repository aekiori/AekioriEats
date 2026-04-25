package com.delivery.order.controller;

import com.delivery.order.auth.AuthenticatedUser;
import com.delivery.order.auth.AuthenticatedUserInfo;
import com.delivery.order.domain.order.Order;
import com.delivery.order.dto.request.CreateOrderDto;
import com.delivery.order.dto.request.UpdateOrderStatusDto;
import com.delivery.order.dto.response.*;
import com.delivery.order.service.order.CreateOrderService;
import com.delivery.order.service.order.GetOrderService;
import com.delivery.order.service.order.GetOrdersService;
import com.delivery.order.service.order.UpdateOrderStatusService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Order", description = "주문 생성, 조회, 상태 변경 API")
public class OrderController {
    private final CreateOrderService createOrderService;
    private final GetOrderService getOrderService;
    private final GetOrdersService getOrdersService;
    private final UpdateOrderStatusService updateOrderStatusService;

    @PostMapping
    @Operation(summary = "주문 생성", description = "idempotency key와 X-User-Id 헤더를 받아 주문을 생성한다.")
    @Parameters({
        @Parameter(
            name = "X-User-Id",
            in = ParameterIn.HEADER,
            description = "Gateway가 주입하는 사용자 ID 헤더",
            required = true,
            example = "1"
        ),
        @Parameter(
            name = "X-Idempotency-Key",
            in = ParameterIn.HEADER,
            description = "주문 생성 멱등성 키(UUID v4)",
            required = true,
            example = "2f1d6df2-188d-4f39-8d3d-8fa2d6af7302"
        )
    })
    public ResponseEntity<CreateOrderResultDto> createOrder(
        @Parameter(hidden = true)
        @AuthenticatedUser AuthenticatedUserInfo authenticatedUser,
        @RequestHeader("X-Idempotency-Key")
        @NotBlank
        @Pattern(regexp = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-4[0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}$")
        String idempotencyKey,
        @RequestBody @Valid CreateOrderDto createOrderDto
    ) {
        CreateOrderResultDto response = createOrderService.createOrder(
            createOrderDto,
            idempotencyKey,
            authenticatedUser.userId()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{orderId}")
    @Operation(summary = "주문 상세 조회", description = "주문 상세와 상태 이력을 함께 조회한다.")
    @Parameters({
        @Parameter(name = "orderId", description = "조회할 주문 ID", required = true, example = "95"),
        @Parameter(
            name = "X-User-Id",
            in = ParameterIn.HEADER,
            description = "Gateway가 주입하는 사용자 ID 헤더",
            required = true,
            example = "1"
        )
    })
    public ResponseEntity<OrderDetailResultDto> getOrder(
        @PathVariable Long orderId,
        @Parameter(hidden = true)
        @AuthenticatedUser AuthenticatedUserInfo authenticatedUser
    ) {
        return ResponseEntity.ok(getOrderService.getOrder(orderId, authenticatedUser.userId()));
    }

    @GetMapping("/{orderId}/status")
    @Operation(summary = "주문 상태 조회", description = "주문 상태만 빠르게 조회한다.")
    @Parameters({
        @Parameter(name = "orderId", description = "조회할 주문 ID", required = true, example = "95"),
        @Parameter(
            name = "X-User-Id",
            in = ParameterIn.HEADER,
            description = "Gateway가 주입하는 사용자 ID 헤더",
            required = true,
            example = "1"
        )
    })
    public ResponseEntity<OrderStatusResultDto> getOrderStatus(
        @PathVariable Long orderId,
        @Parameter(hidden = true)
        @AuthenticatedUser AuthenticatedUserInfo authenticatedUser
    )
    {
        return ResponseEntity.ok(getOrderService.getOrderStatus(orderId, authenticatedUser.userId()));
    }

    @GetMapping
    @Operation(summary = "내 주문 목록 조회", description = "상태, 페이지, 개수 조건으로 내 주문 목록을 조회한다.")
    @Parameters({
        @Parameter(name = "status", description = "필터할 주문 상태", example = "PAID"),
        @Parameter(name = "page", description = "페이지 번호(0부터 시작)", example = "0"),
        @Parameter(name = "limit", description = "페이지 크기", example = "20"),
        @Parameter(
            name = "X-User-Id",
            in = ParameterIn.HEADER,
            description = "Gateway가 주입하는 사용자 ID 헤더",
            required = true,
            example = "1"
        )
    })
    public ResponseEntity<OrderPageResultDto> getMyOrders(
        @RequestParam(required = false) Order.Status status,
        @RequestParam(defaultValue = "0") @Min(0) int page,
        @RequestParam(defaultValue = "20") @Min(1) int limit,
        @Parameter(hidden = true)
        @AuthenticatedUser AuthenticatedUserInfo authenticatedUser
    ) {
        return ResponseEntity.ok(getOrdersService.getOrders(authenticatedUser.userId(), status, page, limit));
    }

    @PostMapping("/{orderId}/cancel")
    @Operation(summary = "주문 취소", description = "주문 소유자가 주문을 취소한다.")
    @Parameters({
        @Parameter(name = "orderId", description = "취소할 주문 ID", required = true, example = "95"),
        @Parameter(
            name = "X-User-Id",
            in = ParameterIn.HEADER,
            description = "Gateway가 주입하는 사용자 ID 헤더",
            required = true,
            example = "1"
        )
    })
    public ResponseEntity<UpdateOrderStatusResultDto> cancelOrder(
        @PathVariable Long orderId,
        @Parameter(hidden = true)
        @AuthenticatedUser AuthenticatedUserInfo authenticatedUser
    ) {
        return ResponseEntity.ok(updateOrderStatusService.cancelOrder(orderId, authenticatedUser.userId()));
    }

    @PatchMapping("/{orderId}/status")
    @Operation(summary = "주문 상태 변경", description = "주문 소유자가 주문 상태를 직접 변경한다.")
    @Parameters({
        @Parameter(name = "orderId", description = "상태를 변경할 주문 ID", required = true, example = "95"),
        @Parameter(
            name = "X-User-Id",
            in = ParameterIn.HEADER,
            description = "Gateway가 주입하는 사용자 ID 헤더",
            required = true,
            example = "1"
        )
    })
    public ResponseEntity<UpdateOrderStatusResultDto> updateOrderStatus(
        @PathVariable Long orderId,
        @Valid @RequestBody UpdateOrderStatusDto updateOrderStatusDto,
        @Parameter(hidden = true)
        @AuthenticatedUser AuthenticatedUserInfo authenticatedUser
    ) {
        return ResponseEntity.ok(
            updateOrderStatusService.updateOrderStatus(orderId, updateOrderStatusDto, authenticatedUser.userId())
        );
    }
}
