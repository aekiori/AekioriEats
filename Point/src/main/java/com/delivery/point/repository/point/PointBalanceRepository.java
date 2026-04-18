package com.delivery.point.repository.point;

import com.delivery.point.domain.point.PointBalance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PointBalanceRepository extends JpaRepository<PointBalance, Long> {
    Optional<PointBalance> findByUserId(Long userId);
}
