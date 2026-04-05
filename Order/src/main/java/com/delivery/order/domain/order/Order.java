package com.delivery.order.domain.order;

import com.delivery.order.domain.order.event.OrderCreatedOutboxEvent;
import com.delivery.order.domain.order.event.OrderStatusChangedOutboxEvent;
import com.delivery.order.domain.order.exception.InvalidOrderStatusTransitionException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.domain.AbstractAggregateRoot;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.List;

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
        @Index(name = "idx_idempotencyKey", columnList = "idempotencyKey")
    }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Order extends AbstractAggregateRoot<Order> {
    public enum Status {PENDING, PAID, FAILED, CANCELLED}

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
        /*
            this.status => targetStatus 로의 변경 규칙
            PENDING -> PAID, FAILED, CANCELLED 만 허용
            PAID -> CANCELLED 만 허용
            FAILED, CANCELLED 에서는 상태 변경 불가
         */
        boolean validTransition =
            (this.status == Status.PENDING && (
                targetStatus == Status.PAID ||
                    targetStatus == Status.FAILED ||
                    targetStatus == Status.CANCELLED
            )) || (this.status == Status.PAID && targetStatus == Status.CANCELLED);

        if (!validTransition) {
            throw new InvalidOrderStatusTransitionException(this.status, targetStatus);
        }
    }
}
