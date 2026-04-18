package com.delivery.point.domain.point;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(
    name = "point_ledger",
    indexes = {
        @Index(name = "uk_point_ledger_idempotency_key", columnList = "idempotency_key", unique = true),
        @Index(name = "idx_point_ledger_user_created_at", columnList = "user_id, created_at"),
        @Index(name = "idx_point_ledger_order_id", columnList = "order_id")
    }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PointLedger {
    public enum Type {DEDUCT, CHARGE, REFUND, EARN}
    public enum Result {SUCCESS, FAILED}

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "order_id")
    private Long orderId;

    @Column(name = "amount", nullable = false)
    private Integer amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private Type type;

    @Enumerated(EnumType.STRING)
    @Column(name = "result", nullable = false, length = 20)
    private Result result;

    @Column(name = "idempotency_key", nullable = false, length = 100)
    private String idempotencyKey;

    @Column(name = "reason", length = 200)
    private String reason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private PointLedger(
        Long userId,
        Long orderId,
        Integer amount,
        Type type,
        Result result,
        String idempotencyKey,
        String reason
    ) {
        this.userId = userId;
        this.orderId = orderId;
        this.amount = amount;
        this.type = type;
        this.result = result;
        this.idempotencyKey = idempotencyKey;
        this.reason = reason;
    }

    public static PointLedger deductionSucceeded(Long userId, Long orderId, Integer amount, String idempotencyKey) {
        return new PointLedger(userId, orderId, amount, Type.DEDUCT, Result.SUCCESS, idempotencyKey, null);
    }

    public static PointLedger deductionFailed(Long userId, Long orderId, Integer amount, String idempotencyKey, String reason) {
        return new PointLedger(userId, orderId, amount, Type.DEDUCT, Result.FAILED, idempotencyKey, reason);
    }

    public static PointLedger charged(Long userId, Integer amount, String idempotencyKey, String reason) {
        return new PointLedger(userId, null, amount, Type.CHARGE, Result.SUCCESS, idempotencyKey, reason);
    }

    @PrePersist
    void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }
}
