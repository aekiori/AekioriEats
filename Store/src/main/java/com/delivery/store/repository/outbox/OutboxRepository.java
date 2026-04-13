package com.delivery.store.repository.outbox;

import com.delivery.store.domain.outbox.Outbox;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OutboxRepository extends JpaRepository<Outbox, Long> {
}
