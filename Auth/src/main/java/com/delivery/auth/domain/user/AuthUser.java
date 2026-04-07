package com.delivery.auth.domain.user;

import com.delivery.auth.domain.user.event.UserCreatedOutboxEvent;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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

@Getter
@Entity
@Table(
    name = "auth_users",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_auth_users_email", columnNames = {"email"})
    }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class AuthUser extends AbstractAggregateRoot<AuthUser> {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, length = 100, unique = true)
    private String email;

    @Column(nullable = false, length = 50)
    private String nickname;

    @Column(nullable = false, length = 30)
    private String role;

    @Column(nullable = false, length = 100)
    private String passwordHash;

    @Column(nullable = false, length = 20)
    private String status;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public static AuthUser createForSignup(String email, String passwordHash) {
        String nickname = deriveDefaultNickname(email);
        return new AuthUser(
            null,
            email,
            nickname,
            "USER",
            passwordHash,
            "ACTIVE"
        );
    }

    private static String deriveDefaultNickname(String email) {
        String localPart = email;
        int atIndex = email.indexOf('@');
        if (atIndex > 0) {
            localPart = email.substring(0, atIndex);
        }

        String trimmed = localPart.trim();
        if (trimmed.isBlank()) {
            return "new-user";
        }

        return trimmed.length() > 50 ? trimmed.substring(0, 50) : trimmed;
    }

    public AuthUser(
        Long userId,
        String email,
        String nickname,
        String role,
        String passwordHash,
        String status
    ) {
        this.userId = userId;
        this.email = email;
        this.nickname = nickname;
        this.role = role;
        this.passwordHash = passwordHash;
        this.status = status;
    }

    public boolean isActive() {
        return "ACTIVE".equalsIgnoreCase(status);
    }

    public void registerCreatedEvent() {
        registerEvent(UserCreatedOutboxEvent.from(this));
    }
}
