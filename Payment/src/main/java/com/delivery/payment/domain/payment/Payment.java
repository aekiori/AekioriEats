package com.delivery.payment.domain.payment;

import com.delivery.payment.dto.event.PaymentRequestedEventDto;
import com.delivery.payment.domain.payment.exception.InvalidPaymentStatusTransitionException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.EnumSet;

@Getter
@Entity
@Table(
    name = "payment",
    indexes = {
        @Index(name = "uk_payment_order_id", columnList = "order_id", unique = true),
        @Index(name = "idx_payment_status_created_at", columnList = "status, created_at")
    }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment {
    public enum Status {
        PENDING,
        SUCCESS,
        FAILED,
        REFUNDED;

        public boolean isTerminal() {
            return this == FAILED || this == REFUNDED;
        }
    }

    public static final String DEFAULT_PG_TYPE = "KAKAOPAY";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false, unique = true)
    private Long orderId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "amount", nullable = false)
    private Integer amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private Status status;

    @Column(name = "pg_type", nullable = false, length = 20)
    private String pgType;

    @Column(name = "pg_transaction_id", length = 100)
    private String pgTransactionId;

    @Column(name = "failed_reason", length = 200)
    private String failedReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    private Payment(
        Long orderId,
        Long userId,
        Integer amount
    ) {
        this.orderId = orderId;
        this.userId = userId;
        this.amount = amount;
        this.status = Status.PENDING;
        this.pgType = DEFAULT_PG_TYPE;
    }

    public static Payment requested(PaymentRequestedEventDto event) {
        return new Payment(
            event.orderId(),
            event.userId(),
            event.finalAmount()
        );
    }

    public void confirmSucceeded(String pgTransactionId) {
        if (this.status == Status.SUCCESS) {
            return;
        }
        requireTransition(Status.SUCCESS, EnumSet.of(Status.PENDING));
        this.pgTransactionId = pgTransactionId;
        this.failedReason = null;
        this.status = Status.SUCCESS;
    }

    public void markFailed(String reason) {
        if (this.status == Status.FAILED) {
            return;
        }
        requireTransition(Status.FAILED, EnumSet.of(Status.PENDING));
        this.status = Status.FAILED;
        this.failedReason = reason;
    }

    public void refund(String reason) {
        if (this.status == Status.REFUNDED) {
            return;
        }
        requireTransition(Status.REFUNDED, EnumSet.of(Status.SUCCESS));
        this.status = Status.REFUNDED;
        this.failedReason = reason;
    }

    private void requireTransition(Status targetStatus, EnumSet<Status> allowedCurrentStatuses) {
        if (!allowedCurrentStatuses.contains(this.status)) {
            throw new InvalidPaymentStatusTransitionException(this.status, targetStatus);
        }
    }

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (this.createdAt == null) {
            this.createdAt = now;
        }
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
