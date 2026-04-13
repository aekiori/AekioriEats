package com.delivery.payment.domain.payment;

import com.delivery.payment.dto.event.PaymentRequestedEventDto;
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
    public enum Status {REQUESTED, SUCCEEDED, FAILED, REFUNDED}

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false, unique = true)
    private Long orderId;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "store_id")
    private Long storeId;

    @Column(name = "total_amount")
    private Integer totalAmount;

    @Column(name = "used_point_amount")
    private Integer usedPointAmount;

    @Column(name = "final_amount")
    private Integer finalAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private Status status;

    @Column(name = "fail_reason", length = 200)
    private String failReason;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    private Payment(
        Long orderId,
        Long userId,
        Long storeId,
        Integer totalAmount,
        Integer usedPointAmount,
        Integer finalAmount
    ) {
        this.orderId = orderId;
        this.userId = userId;
        this.storeId = storeId;
        this.totalAmount = totalAmount;
        this.usedPointAmount = usedPointAmount;
        this.finalAmount = finalAmount;
        this.status = Status.REQUESTED;
    }

    public static Payment requested(PaymentRequestedEventDto event) {
        return new Payment(
            event.orderId(),
            event.userId(),
            event.storeId(),
            event.totalAmount(),
            event.usedPointAmount(),
            event.finalAmount()
        );
    }

    public void markSucceeded() {
        this.status = Status.SUCCEEDED;
        this.failReason = null;
        this.processedAt = LocalDateTime.now();
    }

    public void markFailed(String reason) {
        this.status = Status.FAILED;
        this.failReason = reason;
        this.processedAt = LocalDateTime.now();
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
