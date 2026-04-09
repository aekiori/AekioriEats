package com.delivery.auth.repository.token;

import com.delivery.auth.domain.token.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByToken(String token);
    Optional<RefreshToken> findByTokenAndRevokedFalse(String token);
    List<RefreshToken> findAllByUserIdAndRevokedFalse(Long userId);
}
