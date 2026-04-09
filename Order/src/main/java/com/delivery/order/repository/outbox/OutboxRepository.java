package com.delivery.order.repository.outbox;

import com.delivery.order.domain.outbox.Outbox;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OutboxRepository extends JpaRepository<Outbox, Long> {
    Optional<Outbox> findByEventId(String eventId);

    List<Outbox> findByStatus(Outbox.Status status);

    List<Outbox> findByStatusOrderByCreatedAtAsc(Outbox.Status status);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
        update Outbox o
        set o.status = :targetStatus
        where o.eventId = :eventId
          and o.status = :expectedStatus
        """)
    int updateStatusIfCurrent(
        @Param("eventId") String eventId,
        @Param("expectedStatus") Outbox.Status expectedStatus,
        @Param("targetStatus") Outbox.Status targetStatus
    );
}
