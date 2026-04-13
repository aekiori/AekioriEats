package com.delivery.store.service.event;

import com.delivery.store.domain.store.Store;
import com.delivery.store.domain.store.StoreHour;
import com.delivery.store.dto.event.OrderCreatedEventDto;
import com.delivery.store.repository.store.StoreHolidayRepository;
import com.delivery.store.repository.store.StoreHourRepository;
import com.delivery.store.repository.store.StoreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StoreOrderableValidator {
    private final StoreRepository storeRepository;
    private final StoreHourRepository storeHourRepository;
    private final StoreHolidayRepository storeHolidayRepository;

    @Transactional(readOnly = true)
    public StoreOrderValidationResult validate(OrderCreatedEventDto event) {
        Store store = storeRepository.findById(event.storeId()).orElse(null);

        if (store == null) {
            return StoreOrderValidationResult.reject(
                "STORE_NOT_FOUND",
                "Store was not found for order validation."
            );
        }

        List<StoreHour> hours = storeHourRepository.findByStoreIdOrderByDayOfWeekAsc(store.getId());
        List<LocalDate> holidays = storeHolidayRepository.findHolidayDatesByStoreId(store.getId());

        if (! store.isOpenNow(hours, holidays, LocalDateTime.now())) {
            return StoreOrderValidationResult.reject(
                "STORE_NOT_OPEN",
                "Store is not open. status=%s".formatted(store.getStatus())
            );
        }

        if (event.totalAmount() == null) {
            return StoreOrderValidationResult.reject(
                "ORDER_TOTAL_AMOUNT_MISSING",
                "Order total amount is missing."
            );
        }

        if (event.totalAmount() < store.getMinOrderAmount()) {
            return StoreOrderValidationResult.reject(
                "MIN_ORDER_AMOUNT_NOT_MET",
                "Order total amount is less than store minimum. totalAmount=%d, minOrderAmount=%d"
                    .formatted(event.totalAmount(), store.getMinOrderAmount())
            );
        }

        return StoreOrderValidationResult.pass();
    }
}
