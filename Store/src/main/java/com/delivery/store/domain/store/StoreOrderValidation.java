package com.delivery.store.domain.store;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "store_order_validation")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StoreOrderValidation {
    public enum Result {
        ACCEPTED,
        REJECTED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "store_id", nullable = false)
    private Long storeId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Result result;

    @Column(name = "reject_code", length = 50)
    private String rejectCode;

    @Column(name = "reject_reason", length = 200)
    private String rejectReason;

    @Column(name = "validated_at", nullable = false)
    private LocalDateTime validatedAt;

    private StoreOrderValidation(
        Long orderId,
        Long storeId,
        Result result,
        String rejectCode,
        String rejectReason,
        LocalDateTime validatedAt
    ) {
        this.orderId = orderId;
        this.storeId = storeId;
        this.result = result;
        this.rejectCode = rejectCode;
        this.rejectReason = rejectReason;
        this.validatedAt = validatedAt;
    }

    public static StoreOrderValidation createAccepted(Long orderId, Long storeId, LocalDateTime validatedAt) {
        return new StoreOrderValidation(orderId, storeId, Result.ACCEPTED, null, null, validatedAt);
    }

    public static StoreOrderValidation createRejected(
        Long orderId,
        Long storeId,
        String rejectCode,
        String rejectReason,
        LocalDateTime validatedAt
    ) {
        return new StoreOrderValidation(orderId, storeId, Result.REJECTED, rejectCode, rejectReason, validatedAt);
    }

    public void markAccepted(LocalDateTime validatedAt) {
        this.result = Result.ACCEPTED;
        this.rejectCode = null;
        this.rejectReason = null;
        this.validatedAt = validatedAt;
    }

    public void markRejected(String rejectCode, String rejectReason, LocalDateTime validatedAt) {
        this.result = Result.REJECTED;
        this.rejectCode = rejectCode;
        this.rejectReason = rejectReason;
        this.validatedAt = validatedAt;
    }
}
