package com.delivery.payment.repository.outbox;

import com.delivery.payment.domain.outbox.Outbox;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OutboxRepository extends JpaRepository<Outbox, Long> {
}
