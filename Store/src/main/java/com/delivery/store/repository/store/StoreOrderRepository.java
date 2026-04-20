package com.delivery.store.repository.store;

import com.delivery.store.domain.store.StoreOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StoreOrderRepository extends JpaRepository<StoreOrder, Long> {
    Optional<StoreOrder> findByOrderId(Long orderId);
    List<StoreOrder> findByStoreIdAndStatusOrderByCreatedAtDesc(Long storeId, StoreOrder.Status status);
}
