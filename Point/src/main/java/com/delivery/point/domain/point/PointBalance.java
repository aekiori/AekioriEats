package com.delivery.point.domain.point;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
    name = "point_balance",
    indexes = {
        @Index(name = "uk_point_balance_user_id", columnList = "user_id", unique = true)
    }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PointBalance {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(name = "balance", nullable = false)
    private Integer balance;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    private PointBalance(Long userId, Integer balance) {
        this.userId = userId;
        this.balance = balance;
    }

    public static PointBalance zero(Long userId) {
        return new PointBalance(userId, 0);
    }

    public static PointBalance of(Long userId, Integer balance) {
        return new PointBalance(userId, balance);
    }

    public boolean canDeduct(Integer amount) {
        return amount != null && amount >= 0 && this.balance >= amount;
    }

    public void deduct(Integer amount) {
        if (!canDeduct(amount)) {
            throw new IllegalStateException("Insufficient point balance.");
        }
        this.balance -= amount;
    }

    public void charge(Integer amount) {
        if (amount == null || amount <= 0) {
            throw new IllegalArgumentException("Charge amount must be positive.");
        }
        this.balance += amount;
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
