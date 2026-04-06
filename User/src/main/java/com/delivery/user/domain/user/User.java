package com.delivery.user.domain.user;

import com.delivery.user.domain.user.exception.InvalidUserStatusTransitionException;
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
    name = "users",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_users_email", columnNames = {"email"})
    },
    indexes = {
        @Index(name = "idx_users_status_created_at", columnList = "status, createdAt")
    }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class User {
    public enum Status {ACTIVE, LOCKED, DEACTIVATED}

    private static final Map<Status, Set<Status>> ALLOWED_STATUS_TRANSITIONS = createAllowedStatusTransitions();

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100, unique = true)
    private String email;

    @Column(nullable = false, length = 100)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public User(String email, String passwordHash) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.status = Status.ACTIVE;
    }

    public static User create(String email, String passwordHash) {
        return new User(email, passwordHash);
    }

    public void updateStatus(Status targetStatus) {
        Set<Status> allowedTargets = ALLOWED_STATUS_TRANSITIONS.getOrDefault(this.status, Collections.emptySet());

        if (!allowedTargets.contains(targetStatus)) {
            throw new InvalidUserStatusTransitionException(this.status, targetStatus);
        }

        this.status = targetStatus;
    }

    private static Map<Status, Set<Status>> createAllowedStatusTransitions() {
        Map<Status, Set<Status>> transitions = new EnumMap<>(Status.class);
        transitions.put(Status.ACTIVE, EnumSet.of(Status.LOCKED, Status.DEACTIVATED));
        transitions.put(Status.LOCKED, EnumSet.of(Status.ACTIVE, Status.DEACTIVATED));
        transitions.put(Status.DEACTIVATED, EnumSet.noneOf(Status.class));
        return Collections.unmodifiableMap(transitions);
    }
}
