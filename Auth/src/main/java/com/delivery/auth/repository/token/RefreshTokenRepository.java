package com.delivery.auth.repository.token;

import com.delivery.auth.domain.token.RefreshToken;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByToken(String token);
    Optional<RefreshToken> findByTokenAndRevokedFalse(String token);
    List<RefreshToken> findAllByUserIdAndRevokedFalse(Long userId);

    @Query("SELECT t FROM RefreshToken t WHERE t.expiresAt < :threshold OR t.revoked = true")
    List<RefreshToken> findExpiredOrRevoked(@Param("threshold") LocalDateTime threshold, Pageable pageable);
}
