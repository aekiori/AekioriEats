package com.delivery.store.domain.store;

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
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(
    name = "store_orders",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_store_orders_order_id", columnNames = "order_id")
    },
    indexes = {
        @Index(name = "idx_store_orders_store_status_created_at", columnList = "store_id, status, created_at")
    }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class StoreOrder {
    public enum Status {PENDING, ACCEPTED, REJECTED}

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "store_id", nullable = false)
    private Long storeId;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "final_amount")
    private Integer finalAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Status status;

    @Column(name = "reject_reason", length = 200)
    private String rejectReason;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "decided_at")
    private LocalDateTime decidedAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    private StoreOrder(Long orderId, Long storeId, Long userId, Integer finalAmount, LocalDateTime paidAt) {
        this.orderId = orderId;
        this.storeId = storeId;
        this.userId = userId;
        this.finalAmount = finalAmount;
        this.status = Status.PENDING;
        this.paidAt = paidAt;
    }

    public static StoreOrder pending(Long orderId, Long storeId, Long userId, Integer finalAmount, LocalDateTime paidAt) {
        return new StoreOrder(orderId, storeId, userId, finalAmount, paidAt);
    }

    public void accept(LocalDateTime decidedAt) {
        if (this.status == Status.ACCEPTED) {
            return;
        }
        this.status = Status.ACCEPTED;
        this.rejectReason = null;
        this.decidedAt = decidedAt;
    }

    public void reject(String rejectReason, LocalDateTime decidedAt) {
        if (this.status == Status.REJECTED) {
            return;
        }
        this.status = Status.REJECTED;
        this.rejectReason = rejectReason;
        this.decidedAt = decidedAt;
    }
}
