package com.delivery.order.dto.request;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record CreateOrderDto(
    @Schema(description = "주문 생성 사용자 ID", example = "1")
    @NotNull
    Long userId,

    @Schema(description = "주문 대상 가게 ID", example = "3")
    @NotNull
    Long storeId,

    @Schema(description = "배달 주소", example = "경기 군포시 산본로 100")
    @NotBlank
    String deliveryAddress,

    @Schema(description = "사용 포인트 금액", example = "1000")
    @NotNull
    @Min(0)
    Integer usedPointAmount,

    @ArraySchema(schema = @Schema(implementation = CreateOrderItemDto.class))
    @NotEmpty
    List<@Valid CreateOrderItemDto> items
) {
}
