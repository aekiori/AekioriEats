package com.delivery.order.domain.order;

import com.delivery.order.domain.order.exception.InvalidOrderStatusTransitionException;
import jakarta.persistence.*;
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
public class Order {
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

    public void updateStatus(Status status) {
        validateStatusTransition(status);
        this.status = status;
    }

    /*
        Transition 처리는원래 Service 에 있었지만,
        DDD 상 Order 도메인 엔티티가 처리하는게 맞다.
        여기서 도메인 전용 예외를 떨구고, UpStream layer 에서 해당 예외를 받아 별도처리.
     */
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
