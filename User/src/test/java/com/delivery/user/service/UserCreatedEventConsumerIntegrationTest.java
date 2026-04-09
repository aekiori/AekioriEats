package com.delivery.user.service;

import com.delivery.user.domain.user.User;
import com.delivery.user.repository.user.UserRepository;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class UserCreatedEventConsumerIntegrationTest {
    @Autowired
    private UserCreatedEventConsumer userCreatedEventConsumer;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void tearDown() {
        jdbcTemplate.execute("delete from processed_events");
        userRepository.deleteAll();
    }

    @Test
    void consume_user_created_smt_event_upserts_user() {
        String payload = """
            {
              "eventId": "event-001",
              "eventType": "UserCreated",
              "schemaVersion": 1,
              "occurredAt": "2026-04-07T08:00:00",
              "userId": 101,
              "email": "new-user@example.com",
              "status": "ACTIVE"
            }
            """;

        ConsumerRecord<String, String> record = new ConsumerRecord<>("outbox.event.USER", 0, 0L, "101", payload);

        userCreatedEventConsumer.consume(record);

        User savedUser = userRepository.findById(101L).orElseThrow();

        assertThat(savedUser.getEmail()).isEqualTo("new-user@example.com");
        assertThat(savedUser.getStatus()).isEqualTo(User.Status.ACTIVE);
    }

    @Test
    void consume_duplicate_user_created_event_is_idempotent() {
        String payload = """
            {
              "eventId": "event-dup-001",
              "eventType": "UserCreated",
              "schemaVersion": 1,
              "occurredAt": "2026-04-07T08:01:00",
              "userId": 202,
              "email": "dup-user@example.com",
              "status": "ACTIVE"
            }
            """;

        ConsumerRecord<String, String> record = new ConsumerRecord<>("outbox.event.USER", 0, 0L, "202", payload);

        userCreatedEventConsumer.consume(record);
        userCreatedEventConsumer.consume(record);

        Long count = jdbcTemplate.queryForObject(
            "select count(*) from users where id = ?",
            Long.class,
            202L
        );
        Long processedEventCount = jdbcTemplate.queryForObject(
            "select count(*) from processed_events where event_id = ?",
            Long.class,
            "event-dup-001"
        );

        assertThat(count).isEqualTo(1L);
        assertThat(processedEventCount).isEqualTo(1L);
    }
}
