package com.delivery.order.repository.order;

import com.delivery.order.domain.order.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {
    Page<Order> findByUserId(Long userId, Pageable pageable);

    Page<Order> findByUserIdAndStatus(Long userId, Order.Status status, Pageable pageable);

    Optional<Order> findByIdempotencyKey(String idempotencyKey);
}
