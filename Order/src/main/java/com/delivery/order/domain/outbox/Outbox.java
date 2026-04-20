package com.delivery.order.domain.outbox;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(
    name = "outbox",
    indexes = {
        @Index(name = "idx_status_createdAt", columnList = "status, createdAt"),
        @Index(name = "idx_aggregateType_aggregateId", columnList = "aggregateType, aggregateId"),
        @Index(name = "idx_partitionKey_createdAt", columnList = "partitionKey, createdAt")
    }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Outbox {
    public enum AggregateType {ORDER, PAYMENT, POINT, STORE}
    public enum Status {INIT, PUBLISHED, FAILED}

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String eventId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private AggregateType aggregateType;

    @Column(nullable = false)
    private Long aggregateId;

    @Column(nullable = false, length = 100)
    private String eventType;

    @Lob
    @Column(nullable = false, columnDefinition = "json")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Status status;

    @Column(length = 100)
    private String partitionKey;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public Outbox(
        String eventId,
        AggregateType aggregateType,
        Long aggregateId,
        String eventType,
        String payload,
        Status status,
        String partitionKey
    ) {
        this.eventId = eventId;
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
        this.eventType = eventType;
        this.payload = payload;
        this.status = status;
        this.partitionKey = partitionKey;
    }

    public void updateStatus(Status status) {
        this.status = status;
    }

    public void markFailed() {
        this.status = Status.FAILED;
    }
}
