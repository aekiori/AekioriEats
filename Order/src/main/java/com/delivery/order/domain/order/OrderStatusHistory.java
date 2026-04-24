package com.delivery.order.domain.order;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
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
    name = "order_status_history",
    indexes = {
        @Index(name = "idx_order_status_history_order_id_created_at", columnList = "orderId, createdAt")
    }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class OrderStatusHistory {
    public enum SourceType {
        API,
        STORE_VALIDATION_EVENT,
        PAYMENT_EVENT,
        STORE_DECISION_EVENT
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long orderId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Order.Status fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Order.Status toStatus;

    @Column(length = 500)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private SourceType sourceType;

    @Column(length = 100)
    private String eventId;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public OrderStatusHistory(
        Long orderId,
        Order.Status fromStatus,
        Order.Status toStatus,
        String reason,
        SourceType sourceType,
        String eventId
    ) {
        this.orderId = orderId;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.reason = reason;
        this.sourceType = sourceType;
        this.eventId = eventId;
    }
}
