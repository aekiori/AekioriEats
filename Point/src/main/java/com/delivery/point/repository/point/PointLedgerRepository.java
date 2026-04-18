package com.delivery.point.repository.point;

import com.delivery.point.domain.point.PointLedger;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PointLedgerRepository extends JpaRepository<PointLedger, Long> {
    boolean existsByIdempotencyKey(String idempotencyKey);
}
