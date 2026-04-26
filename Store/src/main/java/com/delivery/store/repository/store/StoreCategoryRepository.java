package com.delivery.store.repository.store;

import com.delivery.store.domain.store.StoreCategory;
import com.delivery.store.domain.store.StoreCategoryId;
import com.delivery.store.dto.response.query.StoreQueryResponseDto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface StoreCategoryRepository extends JpaRepository<StoreCategory, StoreCategoryId> {
    void deleteByStoreId(Long storeId);
    List<StoreCategory> findByStoreId(Long storeId);

    @Query("""
        SELECT c.id, c.name
        FROM Category c
        JOIN StoreCategory sc ON c.id = sc.categoryId
        WHERE sc.storeId = :storeId
        ORDER BY c.id ASC
    """)
    List<StoreQueryResponseDto.CategoryResponseDto> findCategoriesByStoreId(Long storeId);
}
