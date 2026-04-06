package com.delivery.auth.repository.outbox;

import com.delivery.auth.domain.outbox.Outbox;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OutboxRepository extends JpaRepository<Outbox, Long> {
    Optional<Outbox> findByEventId(String eventId);
}
