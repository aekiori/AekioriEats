package com.delivery.store.domain.store;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "store_categories")
@IdClass(StoreCategoryId.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StoreCategory {
    @Id
    @Column(name = "store_id", nullable = false)
    private Long storeId;

    @Id
    @Column(name = "category_id", nullable = false)
    private Long categoryId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public StoreCategory(Long storeId, Long categoryId) {
        this.storeId = storeId;
        this.categoryId = categoryId;
    }

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
