package com.delivery.store.repository.store;

import com.delivery.store.domain.store.StoreHour;
import com.delivery.store.dto.response.query.StoreQueryDtos;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface StoreHourRepository extends JpaRepository<StoreHour, Long> {
    void deleteByStoreId(Long storeId);

    @Query("""
        SELECT h.dayOfWeek AS dayOfWeek, h.openTime AS openTime, h.closeTime AS closeTime
        FROM StoreHour h
        WHERE h.storeId = :storeId
        ORDER BY h.dayOfWeek ASC
    """)
    List<StoreQueryDtos.StoreHourDto> findHoursByStoreId(Long storeId);
}
