package com.delivery.auth.service;

import com.delivery.auth.domain.token.RefreshToken;
import com.delivery.auth.domain.user.AuthUser;
import com.delivery.auth.dto.request.LoginRequestDto;
import com.delivery.auth.dto.request.LogoutRequestDto;
import com.delivery.auth.dto.request.RefreshTokenRequestDto;
import com.delivery.auth.dto.request.SignupRequestDto;
import com.delivery.auth.dto.response.AuthTokenResponseDto;
import com.delivery.auth.dto.response.EmailDuplicateCheckResponseDto;
import com.delivery.auth.exception.ApiException;
import com.delivery.auth.exception.AuthErrorCode;
import com.delivery.auth.repository.token.RefreshTokenRepository;
import com.delivery.auth.repository.user.AuthUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final AuthUserRepository authUserRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthEmailBloomFilter authEmailBloomFilter;
    private final AuthRateLimitService authRateLimitService;
    private final EmailNormalizer emailNormalizer;

    @Value("${auth.jwt.refresh-token-expiration-seconds:1209600}")
    private long refreshTokenExpirationSeconds;

    @Transactional(readOnly = true)
    public EmailDuplicateCheckResponseDto checkEmailDuplicate(String email) {
        String normalizedEmail = emailNormalizer.normalize(email);

        if (!authEmailBloomFilter.shouldCheckDb(normalizedEmail)) {
            return EmailDuplicateCheckResponseDto.from(normalizedEmail, false);
        }

        boolean exists = authUserRepository.existsByEmail(normalizedEmail);

        return EmailDuplicateCheckResponseDto.from(normalizedEmail, exists);
    }

    @Transactional
    public AuthTokenResponseDto signup(SignupRequestDto request, String clientIp) {
        String normalizedEmail = emailNormalizer.normalize(request.email());
        authRateLimitService.validateSignup(normalizedEmail, clientIp);

        if (authEmailBloomFilter.shouldCheckDb(normalizedEmail) && authUserRepository.existsByEmail(normalizedEmail)) {
            throw new ApiException(AuthErrorCode.EMAIL_ALREADY_EXISTS);
        }

        AuthUser authUser;
        try {
            authUser = authUserRepository.save(
                AuthUser.createForSignup(normalizedEmail, passwordEncoder.encode(request.password()))
            );
        } catch (DataIntegrityViolationException exception) {
            throw new ApiException(AuthErrorCode.EMAIL_ALREADY_EXISTS);
        }

        authEmailBloomFilter.put(normalizedEmail);
        authUser.registerCreatedEvent();
        authUserRepository.save(authUser);

        return issueTokens(authUser);
    }

    @Transactional
    public AuthTokenResponseDto login(LoginRequestDto request, String clientIp) {
        String normalizedEmail = emailNormalizer.normalize(request.email());
        authRateLimitService.validateLogin(normalizedEmail, clientIp);

        AuthUser authUser = authUserRepository.findByEmail(normalizedEmail)
            .orElseThrow(() -> new ApiException(AuthErrorCode.INVALID_CREDENTIALS));

        if (!authUser.isActive()) {
            throw new ApiException(AuthErrorCode.USER_NOT_ACTIVE);
        }

        if (!passwordEncoder.matches(request.password(), authUser.getPasswordHash())) {
            throw new ApiException(AuthErrorCode.INVALID_CREDENTIALS);
        }

        authRateLimitService.clearLoginAccountLimit(normalizedEmail);
        return issueTokens(authUser);
    }

    @Transactional(noRollbackFor = ApiException.class)
    public AuthTokenResponseDto refresh(RefreshTokenRequestDto request) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(request.refreshToken())
            .orElseThrow(() -> new ApiException(AuthErrorCode.INVALID_REFRESH_TOKEN));

        if (Boolean.TRUE.equals(refreshToken.getRevoked())) {
            revokeActiveRefreshTokens(refreshToken.getUserId());
            throw new ApiException(AuthErrorCode.REFRESH_TOKEN_REUSE_DETECTED);
        }

        if (refreshToken.isExpired(LocalDateTime.now())) {
            refreshToken.revoke();
            throw new ApiException(AuthErrorCode.REFRESH_TOKEN_EXPIRED);
        }

        AuthUser authUser = authUserRepository.findById(refreshToken.getUserId())
            .orElseThrow(() -> new ApiException(AuthErrorCode.USER_NOT_FOUND));

        refreshToken.revoke();
        return issueTokens(authUser);
    }

    @Transactional
    public void logout(LogoutRequestDto request) {
        RefreshToken refreshToken = refreshTokenRepository.findByTokenAndRevokedFalse(request.refreshToken())
            .orElse(null);

        if (refreshToken != null) {
            refreshToken.revoke();
        }
    }

    private AuthTokenResponseDto issueTokens(AuthUser authUser) {
        String accessToken = jwtTokenProvider.createAccessToken(authUser);
        String refreshTokenValue = UUID.randomUUID().toString();
        LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(refreshTokenExpirationSeconds);

        refreshTokenRepository.save(new RefreshToken(authUser.getUserId(), refreshTokenValue, expiresAt));

        return new AuthTokenResponseDto(
            accessToken,
            refreshTokenValue,
            "Bearer",
            jwtTokenProvider.getAccessTokenExpirationSeconds()
        );
    }

    private void revokeActiveRefreshTokens(Long userId) {
        refreshTokenRepository.findAllByUserIdAndRevokedFalse(userId)
            .forEach(RefreshToken::revoke);
    }
}
