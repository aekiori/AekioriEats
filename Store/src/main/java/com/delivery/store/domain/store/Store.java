package com.delivery.store.domain.store;

import com.delivery.store.domain.store.exception.InvalidStoreStatusTransitionException;
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
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

@Getter
@Entity
@Table(
    name = "stores",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_stores_owner_user_id_name", columnNames = {"owner_user_id", "name"})
    },
    indexes = {
        @Index(
            name = "idx_stores_owner_user_id_status_created_at",
            columnList = "owner_user_id, status, created_at"
        )
    }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Store {
    public enum Status {OPEN, CLOSED, BREAK}

    private static final Map<Status, Set<Status>> ALLOWED_STATUS_TRANSITIONS = createAllowedStatusTransitions();

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "owner_user_id", nullable = false)
    private Long ownerUserId;

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status;

    @Column(name = "status_override", nullable = false)
    private boolean statusOverride;

    @Column(name = "min_order_amount", nullable = false)
    private int minOrderAmount;

    @Column(name = "delivery_tip", nullable = false)
    private int deliveryTip;

    @Column(name = "store_logo_url", length = 512)
    private String storeLogoUrl;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    private Store(Long ownerUserId, String name) {
        this.ownerUserId = ownerUserId;
        this.name = name;
        this.status = Status.OPEN;
        this.statusOverride = false;
        this.minOrderAmount = 0;
        this.deliveryTip = 0;
        this.storeLogoUrl = null;
    }

    public static Store create(Long ownerUserId, String name) {
        return new Store(ownerUserId, name);
    }

    public void updateName(String name) {
        this.name = name;
    }

    public void updateDeliveryPolicy(int minOrderAmount, int deliveryTip) {
        this.minOrderAmount = minOrderAmount;
        this.deliveryTip = deliveryTip;
    }

    public void updateStoreLogoUrl(String storeLogoUrl) {
        this.storeLogoUrl = storeLogoUrl;
    }

    public void updateStatus(Status targetStatus) {
        Set<Status> allowedTargets = ALLOWED_STATUS_TRANSITIONS.getOrDefault(this.status, Collections.emptySet());

        if (!allowedTargets.contains(targetStatus)) {
            throw new InvalidStoreStatusTransitionException(this.status, targetStatus);
        }

        this.status = targetStatus;
    }

    private static Map<Status, Set<Status>> createAllowedStatusTransitions() {
        Map<Status, Set<Status>> transitions = new EnumMap<>(Status.class);
        transitions.put(Status.OPEN, EnumSet.of(Status.CLOSED, Status.BREAK));
        transitions.put(Status.CLOSED, EnumSet.of(Status.OPEN, Status.BREAK));
        transitions.put(Status.BREAK, EnumSet.of(Status.OPEN, Status.CLOSED));
        return Collections.unmodifiableMap(transitions);
    }
}
