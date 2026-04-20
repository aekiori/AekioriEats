package com.delivery.user.service.event;

import com.delivery.user.domain.user.User;
import com.delivery.user.dto.event.UserCreatedEventDto;
import com.delivery.user.exception.UnprocessableEventException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class UserProjectionService {
    private static final String INSERT_PROCESSED_EVENT_SQL = """
        INSERT INTO processed_events (event_id, event_type, aggregate_id, schema_version, occurred_at, processed_at)
        VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP(6))
        """;

    private static final String UPSERT_SQL = """
        INSERT INTO users (id, email, status, created_at, updated_at)
        VALUES (?, ?, ?, CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP())
        ON DUPLICATE KEY UPDATE
            id = id
        """;

    private final JdbcTemplate jdbcTemplate;

    @Transactional
    public boolean upsertUserCreated(UserCreatedEventDto event) {
        if (!tryInsertProcessedEvent(event)) {
            return false;
        }

        String normalizedEmail = normalizeEmail(event.email());
        String normalizedStatus = normalizeStatus(event.status());

        jdbcTemplate.update(UPSERT_SQL, event.userId(), normalizedEmail, normalizedStatus);
        return true;
    }

    private boolean tryInsertProcessedEvent(UserCreatedEventDto event) {
        try {
            jdbcTemplate.update(
                INSERT_PROCESSED_EVENT_SQL,
                event.eventId(),
                event.eventType(),
                event.userId(),
                event.schemaVersion(),
                toTimestamp(event.occurredAt())
            );
            return true;
        } catch (DuplicateKeyException exception) {
            return false;
        }
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeStatus(String status) {
        try {
            return User.Status.valueOf(status.trim().toUpperCase(Locale.ROOT)).name();
        } catch (IllegalArgumentException exception) {
            throw new UnprocessableEventException("Unknown user status: " + status);
        }
    }
    private Timestamp toTimestamp(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return Timestamp.valueOf(dateTime);
    }
}
