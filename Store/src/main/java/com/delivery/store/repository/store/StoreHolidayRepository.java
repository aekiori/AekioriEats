package com.delivery.store.repository.store;

import com.delivery.store.domain.store.StoreHoliday;
import com.delivery.store.dto.response.query.StoreQueryDtos;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;

public interface StoreHolidayRepository extends JpaRepository<StoreHoliday, Long> {
    void deleteByStoreId(Long storeId);
    List<StoreHoliday> findByStoreIdOrderByHolidayDateAsc(Long storeId);

    // 영업 판단용
    boolean existsByStoreIdAndHolidayDate(Long storeId, LocalDate holidayDate);

    @Query("""
        SELECT h.holidayDate AS date, h.reason AS reason
        FROM StoreHoliday h
        WHERE h.storeId = :storeId
        AND h.holidayDate >= :from
        ORDER BY h.holidayDate ASC
    """) // 가게 상세 표시용 (앞으로 30일치만)
    List<StoreQueryDtos.StoreHolidayDto> findUpcomingHolidaysByStoreId(Long storeId, LocalDate from);

}
