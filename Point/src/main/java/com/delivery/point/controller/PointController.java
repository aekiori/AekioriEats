package com.delivery.point.controller;

import com.delivery.point.auth.AuthenticatedUser;
import com.delivery.point.auth.AuthenticatedUserInfo;
import com.delivery.point.dto.request.ChargePointRequest;
import com.delivery.point.dto.response.PointBalanceResponse;
import com.delivery.point.service.point.PointService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/points")
@Tag(name = "Point", description = "포인트 잔액 조회 및 충전 API")
public class PointController {
    private final PointService pointService;

    @GetMapping("/users/{userId}/balance")
    @Operation(summary = "포인트 잔액 조회", description = "사용자 포인트 잔액을 조회한다.")
    public PointBalanceResponse getBalance(
        @PathVariable Long userId,
        @Parameter(hidden = true)
        @AuthenticatedUser AuthenticatedUserInfo authenticatedUser
    ) {
        return pointService.getBalance(userId, authenticatedUser.userId());
    }

    @PostMapping("/users/{userId}/charge")
    @Operation(summary = "포인트 충전", description = "사용자 포인트를 충전한다.")
    public PointBalanceResponse charge(
        @Parameter(description = "포인트를 충전할 사용자 ID", required = true, example = "1")
        @PathVariable Long userId,
        @RequestBody ChargePointRequest request,
        @Parameter(hidden = true)
        @AuthenticatedUser AuthenticatedUserInfo authenticatedUser
    ) {
        return pointService.charge(userId, request, authenticatedUser.userId());
    }
}
