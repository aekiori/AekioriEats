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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
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

    public boolean isOpenNow(List<StoreHour> hoursList, List<LocalDate> holidays, LocalDateTime now) {
        if (! this.status.equals(Status.OPEN)) {
            return false;
        }

        if (holidays.contains(now.toLocalDate())) {
            return false;
        }

        int today = now.getDayOfWeek().getValue();   // 1~7
        int yesterday = today == 1 ? 7 : today - 1;
        LocalTime currentTime = now.toLocalTime();

        for (StoreHour h : hoursList) {
            if (h.getOpenTime() == null || h.getCloseTime() == null) {
                continue;
            }

            if (h.getDayOfWeek() == today && isOpenAtTime(h, currentTime)) { // 오늘 기준
                return true;
            }

            // 18:00 ~ 03:00 같은 심야영업 이뤄지는경우
            if (h.getDayOfWeek() == yesterday && isLateNight(h) && currentTime.isBefore(h.getCloseTime())) {
                return true;
            }
        }

        return false;
    }

    private boolean isOpenAtTime(StoreHour h, LocalTime now) {
        if (isLateNight(h)) {
            return !now.isBefore(h.getOpenTime()) || now.isBefore(h.getCloseTime());
        }
        return !now.isBefore(h.getOpenTime()) && now.isBefore(h.getCloseTime());
    }

    private boolean isLateNight(StoreHour h) {
        return h.getCloseTime().isBefore(h.getOpenTime());
    }
}
