package com.delivery.store.domain.store;

import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@EqualsAndHashCode
@NoArgsConstructor
public class StoreCategoryId implements Serializable {
    private Long storeId;
    private Long categoryId;

    public StoreCategoryId(Long storeId, Long categoryId) {
        this.storeId = storeId;
        this.categoryId = categoryId;
    }
}
