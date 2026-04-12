package com.delivery.store.service.store;

import com.delivery.store.domain.store.Store;
import com.delivery.store.domain.store.StoreHoliday;
import com.delivery.store.domain.store.StoreHour;
import com.delivery.store.dto.request.owner.ReplaceStoreHolidaysRequest;
import com.delivery.store.dto.request.owner.ReplaceStoreHoursRequest;
import com.delivery.store.dto.request.owner.StoreHourRequest;
import com.delivery.store.dto.response.StoreDetailResultDto;
import com.delivery.store.exception.ApiException;
import com.delivery.store.repository.store.StoreHolidayRepository;
import com.delivery.store.repository.store.StoreHourRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class StoreScheduleService {
    private final StoreDomainSupport storeDomainSupport;
    private final StoreHourRepository storeHourRepository;
    private final StoreHolidayRepository storeHolidayRepository;

    @Transactional
    public StoreDetailResultDto replaceStoreHours(
        Long storeId,
        ReplaceStoreHoursRequest request,
        long authenticatedUserId,
        String authenticatedUserRole
    ) {
        Store store = storeDomainSupport.requireOwnedStore(storeId, authenticatedUserId, authenticatedUserRole);
        validateWeeklyHours(request.weeklyHours());
        storeHourRepository.deleteByStoreId(storeId);
        List<StoreHour> hours = request.weeklyHours().stream()
            .sorted((left, right) -> Integer.compare(left.dayOfWeek(), right.dayOfWeek()))
            .map(hour -> toStoreHour(storeId, hour))
            .toList();
        storeHourRepository.saveAll(hours);
        return StoreDetailResultDto.from(store);
    }

    @Transactional
    public StoreDetailResultDto replaceStoreHolidays(
        Long storeId,
        ReplaceStoreHolidaysRequest request,
        long authenticatedUserId,
        String authenticatedUserRole
    ) {
        Store store = storeDomainSupport.requireOwnedStore(storeId, authenticatedUserId, authenticatedUserRole);
        storeHolidayRepository.deleteByStoreId(storeId);
        List<StoreHoliday> holidays = request.holidays().stream()
            .map(holiday -> StoreHoliday.create(
                storeId,
                holiday.date(),
                holiday.reason() == null ? null : holiday.reason().trim()
            ))
            .toList();
        storeHolidayRepository.saveAll(holidays);
        return StoreDetailResultDto.from(store);
    }

    private void validateWeeklyHours(List<StoreHourRequest> weeklyHours) {
        Set<Integer> duplicatedDays = new HashSet<>();
        Set<Integer> seenDays = new HashSet<>();
        for (StoreHourRequest weeklyHour : weeklyHours) {
            if (weeklyHour.dayOfWeek() < 1 || weeklyHour.dayOfWeek() > 7) {
                throw new ApiException(
                    "INVALID_STORE_HOURS",
                    "dayOfWeek must be in range 1..7.",
                    HttpStatus.BAD_REQUEST
                );
            }
            if (!seenDays.add(weeklyHour.dayOfWeek())) {
                duplicatedDays.add(weeklyHour.dayOfWeek());
            }
            boolean hasOpenTime = weeklyHour.openTime() != null;
            boolean hasCloseTime = weeklyHour.closeTime() != null;

            if (hasOpenTime != hasCloseTime) {
                throw new ApiException(
                    "INVALID_STORE_HOURS",
                    "openTime and closeTime must both be provided or both be null.",
                    HttpStatus.BAD_REQUEST
                );
            }
        }
        if (!duplicatedDays.isEmpty()) {
            throw new ApiException(
                "INVALID_STORE_HOURS",
                "Duplicate dayOfWeek is not allowed.",
                HttpStatus.BAD_REQUEST
            );
        }
    }

    private StoreHour toStoreHour(Long storeId, StoreHourRequest request) {
        LocalTime openTime = parseNullableTime(request.openTime(), "openTime");
        LocalTime closeTime = parseNullableTime(request.closeTime(), "closeTime");
        boolean isClosed = openTime == null && closeTime == null;

        return StoreHour.create(
            storeId,
            request.dayOfWeek(),
            openTime,
            closeTime,
            isClosed
        );
    }

    private LocalTime parseNullableTime(String rawTime, String fieldName) {
        if (rawTime == null) {
            return null;
        }
        return parseTime(rawTime, fieldName);
    }

    private LocalTime parseTime(String rawTime, String fieldName) {
        if (rawTime == null || rawTime.isBlank()) {
            throw new ApiException(
                "INVALID_STORE_HOURS",
                fieldName + " is invalid.",
                HttpStatus.BAD_REQUEST
            );
        }
        try {
            return LocalTime.parse(rawTime);
        } catch (Exception exception) {
            throw new ApiException(
                "INVALID_STORE_HOURS",
                fieldName + " is invalid.",
                HttpStatus.BAD_REQUEST
            );
        }
    }
}
