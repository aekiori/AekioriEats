package com.delivery.point.repository.outbox;

import com.delivery.point.domain.outbox.Outbox;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OutboxRepository extends JpaRepository<Outbox, Long> {
}
