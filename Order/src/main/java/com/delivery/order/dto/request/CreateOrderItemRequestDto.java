package com.delivery.order.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateOrderItemRequestDto(
    @Schema(description = "메뉴 ID", example = "30001")
    @NotNull
    Long menuId,

    @Schema(description = "메뉴 이름", example = "마왕치킨")
    @NotBlank
    String menuName,

    @Schema(description = "단가", example = "19900")
    @NotNull
    @Min(1)
    Integer unitPrice,

    @Schema(description = "수량", example = "1")
    @NotNull
    @Min(1)
    Integer quantity
) {
}
