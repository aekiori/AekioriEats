package com.delivery.store.repository.store;

import com.delivery.store.domain.store.StoreOrderValidation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StoreOrderValidationRepository extends JpaRepository<StoreOrderValidation, Long> {
    Optional<StoreOrderValidation> findTopByOrderIdOrderByIdDesc(Long orderId);
}
