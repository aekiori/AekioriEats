package com.delivery.point.dto.response;

public record PointBalanceResponse(
    Long userId,
    Integer balance
) {
}
