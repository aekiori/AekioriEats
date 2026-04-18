package com.delivery.point.controller;

import com.delivery.point.dto.request.ChargePointRequest;
import com.delivery.point.dto.response.PointBalanceResponse;
import com.delivery.point.service.point.PointService;
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
public class PointController {
    private final PointService pointService;

    @GetMapping("/users/{userId}/balance")
    public PointBalanceResponse getBalance(@PathVariable Long userId) {
        return pointService.getBalance(userId);
    }

    @PostMapping("/users/{userId}/charge")
    public PointBalanceResponse charge(
        @PathVariable Long userId,
        @RequestBody ChargePointRequest request
    ) {
        return pointService.charge(userId, request);
    }
}
