package com.delivery.store.dto.response.query;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public final class StoreQueryResponseDto {
    private StoreQueryResponseDto() {
    }

    public record StoreSearchPageDto(
        List<StoreSearchItemDto> content,
        int page,
        int size,
        long totalElements,
        int totalPages
    ) {
    }

    public record StoreSearchItemDto(
        Long storeId,
        String name,
        String status,
        DeliveryPolicyDto deliveryPolicy,
        String storeLogoUrl,
        List<String> matchedBy
    ) {
    }

    public record StoreDetailDto(
        Long storeId,
        Long ownerUserId,
        String name,
        String status,
        DeliveryPolicyDto deliveryPolicy,
        ImagesDto images,
        List<CategoryDto> categories,
        List<StoreHourDto> operatingHours,
        List<StoreHolidayDto> holidays,
        List<MenuGroupDto> menuGroups
    ) {
    }

    public record DeliveryPolicyDto(
        int minOrderAmount,
        int deliveryTip
    ) {
    }

    public record ImagesDto(
        String storeLogoUrl
    ) {
    }

    public record CategoryDto(
        Long id,
        String name
    ) {
    }

    public record StoreHourDto(
        int dayOfWeek,
        LocalTime openTime,
        LocalTime closeTime
    ) {
    }

    public record StoreHolidayDto(
        LocalDate date,
        String reason
    ) {
    }

    public record MenuGroupDto(
        Long id,
        String name,
        List<MenuDto> menus
    ) {
    }

    public record MenuDto(
        Long id,
        String name,
        String description,
        int price,
        boolean isAvailable,
        boolean isSoldOut,
        String imageUrl,
        List<TagDto> tags,
        List<OptionGroupDto> optionGroups
    ) {
    }

    public record TagDto(
        Long id,
        String name
    ) {
    }

    public record OptionGroupDto(
        String name,
        boolean isRequired,
        boolean isMultiple,
        int minSelectCount,
        int maxSelectCount,
        List<OptionDto> options
    ) {
    }

    public record OptionDto(
        String name,
        int extraPrice,
        boolean isAvailable
    ) {
    }
}
