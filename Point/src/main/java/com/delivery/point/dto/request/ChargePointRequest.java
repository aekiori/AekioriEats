package com.delivery.point.dto.request;

public record ChargePointRequest(
    Integer amount,
    String reason
) {
}
