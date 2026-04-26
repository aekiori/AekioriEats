package com.delivery.order.domain.order;

import com.delivery.order.domain.order.event.OrderCreatedOutboxEvent;
import com.delivery.order.domain.order.event.OrderStatusChangedOutboxEvent;
import com.delivery.order.domain.order.exception.InvalidOrderStatusTransitionException;
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
import org.springframework.data.domain.AbstractAggregateRoot;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Getter
@Entity
@Table(
    name = "orders",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_idempotencyKey", columnNames = {"idempotencyKey"})
    },
    indexes = {
        @Index(name = "idx_userId_createdAt", columnList = "userId, createdAt"),
        @Index(name = "idx_userId_status_createdAt", columnList = "userId, status, createdAt"),
        @Index(name = "idx_idempotencyKey", columnList = "idempotencyKey"),
        @Index(name = "idx_status_updatedAt", columnList = "status, updatedAt")
    }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Order extends AbstractAggregateRoot<Order> {
    public enum Status {PENDING, PAYMENT_PENDING, PAID, ACCEPTED, REFUND_PENDING, REFUNDED, FAILED, CANCELLED}

    private static final Map<Status, Set<Status>> ALLOWED_STATUS_TRANSITIONS = createAllowedStatusTransitions();

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Long storeId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Status status;

    @Column(nullable = false, length = 500)
    private String deliveryAddress;

    @Column(nullable = false)
    private Integer totalAmount;

    @Column(nullable = false)
    private Integer usedPointAmount;

    @Column(nullable = false)
    private Integer finalAmount;

    @Column(unique = true, length = 100)
    private String idempotencyKey;

    @Column(length = 100)
    private String requestHash;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public Order(
        Long userId,
        Long storeId,
        Status status,
        String deliveryAddress,
        Integer totalAmount,
        Integer usedPointAmount,
        Integer finalAmount,
        String idempotencyKey,
        String requestHash
    ) {
        this.userId = userId;
        this.storeId = storeId;
        this.status = status;
        this.deliveryAddress = deliveryAddress;
        this.totalAmount = totalAmount;
        this.usedPointAmount = usedPointAmount;
        this.finalAmount = finalAmount;
        this.idempotencyKey = idempotencyKey;
        this.requestHash = requestHash;
    }

    public void updateStatus(Status targetStatus, String reason) {
        validateStatusTransition(targetStatus);

        Status currentStatus = this.status;
        this.status = targetStatus;

        registerEvent(OrderStatusChangedOutboxEvent.from(this, currentStatus, targetStatus, reason));
    }

    public void registerCreatedEvent(List<OrderItem> items) {
        registerEvent(OrderCreatedOutboxEvent.from(this, items));
    }

    private void validateStatusTransition(Status targetStatus) {
        Set<Status> allowedTargets = ALLOWED_STATUS_TRANSITIONS.getOrDefault(this.status, Collections.emptySet());

        if (!allowedTargets.contains(targetStatus)) {
            throw new InvalidOrderStatusTransitionException(this.status, targetStatus);
        }
    }

    private static Map<Status, Set<Status>> createAllowedStatusTransitions() {
        Map<Status, Set<Status>> transitions = new EnumMap<>(Status.class);
        transitions.put(Status.PENDING, EnumSet.of(Status.PAYMENT_PENDING, Status.PAID, Status.FAILED, Status.CANCELLED));
        transitions.put(Status.PAYMENT_PENDING, EnumSet.of(Status.PAID, Status.FAILED, Status.CANCELLED));
        transitions.put(Status.PAID, EnumSet.of(Status.ACCEPTED, Status.REFUND_PENDING, Status.CANCELLED));
        transitions.put(Status.ACCEPTED, EnumSet.noneOf(Status.class));
        transitions.put(Status.REFUND_PENDING, EnumSet.of(Status.REFUNDED, Status.CANCELLED));
        transitions.put(Status.REFUNDED, EnumSet.noneOf(Status.class));
        transitions.put(Status.FAILED, EnumSet.noneOf(Status.class));
        transitions.put(Status.CANCELLED, EnumSet.noneOf(Status.class));

        return Collections.unmodifiableMap(transitions);
    }
}
