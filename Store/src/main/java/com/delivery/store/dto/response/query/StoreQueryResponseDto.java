package com.delivery.store.dto.response.query;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public final class StoreQueryResponseDto {
    private StoreQueryResponseDto() {
    }

    public record StoreSearchPageResponseDto(
        List<StoreSearchItemResponseDto> content,
        int page,
        int size,
        long totalElements,
        int totalPages
    ) {
    }

    public record StoreSearchItemResponseDto(
        Long storeId,
        String name,
        String status,
        DeliveryPolicyResponseDto deliveryPolicy,
        String storeLogoUrl,
        List<String> matchedBy
    ) {
    }

    public record StoreDetailResponseDto(
        Long storeId,
        Long ownerUserId,
        String name,
        String status,
        DeliveryPolicyResponseDto deliveryPolicy,
        ImagesResponseDto images,
        List<CategoryResponseDto> categories,
        List<StoreHourResponseDto> operatingHours,
        List<StoreHolidayResponseDto> holidays,
        List<MenuGroupResponseDto> menuGroups
    ) {
    }

    public record DeliveryPolicyResponseDto(
        int minOrderAmount,
        int deliveryTip
    ) {
    }

    public record ImagesResponseDto(
        String storeLogoUrl
    ) {
    }

    public record CategoryResponseDto(
        Long id,
        String name
    ) {
    }

    public record StoreHourResponseDto(
        int dayOfWeek,
        LocalTime openTime,
        LocalTime closeTime
    ) {
    }

    public record StoreHolidayResponseDto(
        LocalDate date,
        String reason
    ) {
    }

    public record MenuGroupResponseDto(
        Long id,
        String name,
        List<MenuResponseDto> menus
    ) {
    }

    public record MenuResponseDto(
        Long id,
        String name,
        String description,
        int price,
        boolean isAvailable,
        boolean isSoldOut,
        String imageUrl,
        List<TagResponseDto> tags,
        List<OptionGroupResponseDto> optionGroups
    ) {
    }

    public record TagResponseDto(
        Long id,
        String name
    ) {
    }

    public record OptionGroupResponseDto(
        String name,
        boolean isRequired,
        boolean isMultiple,
        int minSelectCount,
        int maxSelectCount,
        List<OptionResponseDto> options
    ) {
    }

    public record OptionResponseDto(
        String name,
        int extraPrice,
        boolean isAvailable
    ) {
    }
}
