package com.delivery.order.repository.order;

import com.delivery.order.domain.order.Order;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {
    Page<Order> findByUserId(Long userId, Pageable pageable);

    Page<Order> findByUserIdAndStatus(Long userId, Order.Status status, Pageable pageable);

    Optional<Order> findByIdempotencyKey(String idempotencyKey);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select o
        from Order o
        where o.status = :status
          and o.updatedAt < :cutoff
        order by o.updatedAt asc
        """)
    List<Order> findTimedOutOrdersForUpdate(
        @Param("status") Order.Status status,
        @Param("cutoff") LocalDateTime cutoff,
        Pageable pageable
    );
}
