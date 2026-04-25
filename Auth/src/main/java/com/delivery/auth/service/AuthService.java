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
import com.delivery.auth.repository.token.RefreshTokenRepository;
import com.delivery.auth.repository.user.AuthUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Locale;
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

    @Value("${auth.jwt.refresh-token-expiration-seconds:1209600}")
    private long refreshTokenExpirationSeconds;

    @Transactional(readOnly = true)
    public EmailDuplicateCheckResponseDto checkEmailDuplicate(String email) {
        String normalizedEmail = normalizeEmail(email);

        if (!authEmailBloomFilter.shouldCheckDb(normalizedEmail)) {
            return EmailDuplicateCheckResponseDto.from(normalizedEmail, false);
        }

        boolean exists = authUserRepository.existsByEmail(normalizedEmail);

        return EmailDuplicateCheckResponseDto.from(normalizedEmail, exists);
    }

    @Transactional
    public AuthTokenResponseDto signup(SignupRequestDto request, String clientIp) {
        String normalizedEmail = normalizeEmail(request.email());
        authRateLimitService.validateSignup(normalizedEmail, clientIp);

        if (authEmailBloomFilter.shouldCheckDb(normalizedEmail) && authUserRepository.existsByEmail(normalizedEmail)) {
            throw new ApiException(
                "AUTH_EMAIL_ALREADY_EXISTS",
                "Email is already registered.",
                HttpStatus.CONFLICT
            );
        }

        AuthUser authUser;
        try {
            authUser = authUserRepository.save(
                AuthUser.createForSignup(normalizedEmail, passwordEncoder.encode(request.password()))
            );
        } catch (DataIntegrityViolationException exception) {
            throw new ApiException(
                "AUTH_EMAIL_ALREADY_EXISTS",
                "Email is already registered.",
                HttpStatus.CONFLICT
            );
        }

        authEmailBloomFilter.put(normalizedEmail);
        authUser.registerCreatedEvent();
        authUserRepository.save(authUser);

        return issueTokens(authUser);
    }

    @Transactional
    public AuthTokenResponseDto login(LoginRequestDto request, String clientIp) {
        String normalizedEmail = normalizeEmail(request.email());
        authRateLimitService.validateLogin(normalizedEmail, clientIp);

        AuthUser authUser = authUserRepository.findByEmail(normalizedEmail)
            .orElseThrow(() -> new ApiException(
                "AUTH_INVALID_CREDENTIALS",
                "Email or password is invalid.",
                HttpStatus.UNAUTHORIZED
            ));

        if (!authUser.isActive()) {
            throw new ApiException(
                "AUTH_USER_NOT_ACTIVE",
                "User is not active.",
                HttpStatus.FORBIDDEN
            );
        }

        if (!passwordEncoder.matches(request.password(), authUser.getPasswordHash())) {
            throw new ApiException(
                "AUTH_INVALID_CREDENTIALS",
                "Email or password is invalid.",
                HttpStatus.UNAUTHORIZED
            );
        }

        authRateLimitService.clearLoginAccountLimit(normalizedEmail);
        return issueTokens(authUser);
    }

    @Transactional(noRollbackFor = ApiException.class)
    public AuthTokenResponseDto refresh(RefreshTokenRequestDto request) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(request.refreshToken())
            .orElseThrow(() -> new ApiException(
                "AUTH_INVALID_REFRESH_TOKEN",
                "Refresh token is invalid.",
                HttpStatus.UNAUTHORIZED
            ));

        if (Boolean.TRUE.equals(refreshToken.getRevoked())) {
            revokeActiveRefreshTokens(refreshToken.getUserId());
            throw new ApiException(
                "AUTH_REFRESH_TOKEN_REUSE_DETECTED",
                "Refresh token reuse detected. Please login again.",
                HttpStatus.UNAUTHORIZED
            );
        }

        if (refreshToken.isExpired(LocalDateTime.now())) {
            refreshToken.revoke();
            throw new ApiException(
                "AUTH_REFRESH_TOKEN_EXPIRED",
                "Refresh token is expired.",
                HttpStatus.UNAUTHORIZED
            );
        }

        AuthUser authUser = authUserRepository.findById(refreshToken.getUserId())
            .orElseThrow(() -> new ApiException(
                "AUTH_USER_NOT_FOUND",
                "User was not found.",
                HttpStatus.UNAUTHORIZED
            ));

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

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private void revokeActiveRefreshTokens(Long userId) {
        refreshTokenRepository.findAllByUserIdAndRevokedFalse(userId)
            .forEach(RefreshToken::revoke);
    }
}
