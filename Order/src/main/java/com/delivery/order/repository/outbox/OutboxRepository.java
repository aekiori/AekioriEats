package com.delivery.order.repository.outbox;

import com.delivery.order.domain.outbox.Outbox;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OutboxRepository extends JpaRepository<Outbox, Long> {
    Optional<Outbox> findByEventId(String eventId);

    List<Outbox> findByStatus(Outbox.Status status);

    List<Outbox> findByStatusOrderByCreatedAtAsc(Outbox.Status status);
}
