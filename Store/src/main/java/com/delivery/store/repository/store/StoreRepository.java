package com.delivery.store.repository.store;

import com.delivery.store.domain.store.Store;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface StoreRepository extends JpaRepository<Store, Long> {
    Page<Store> findByNameContainingIgnoreCase(String name, Pageable pageable);
    List<Store> findByOwnerUserIdOrderByCreatedAtDesc(Long ownerUserId);
}
