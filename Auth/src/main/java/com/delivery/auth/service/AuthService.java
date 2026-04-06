package com.delivery.auth.service;

import com.delivery.auth.domain.token.RefreshToken;
import com.delivery.auth.domain.user.AuthUser;
import com.delivery.auth.domain.user.event.UserCreatedOutboxEvent;
import com.delivery.auth.dto.request.LoginRequestDto;
import com.delivery.auth.dto.request.LogoutRequestDto;
import com.delivery.auth.dto.request.RefreshTokenRequestDto;
import com.delivery.auth.dto.request.SignupRequestDto;
import com.delivery.auth.dto.response.AuthTokenResponseDto;
import com.delivery.auth.dto.response.EmailDuplicateCheckResultDto;
import com.delivery.auth.exception.ApiException;
import com.delivery.auth.repository.token.RefreshTokenRepository;
import com.delivery.auth.repository.user.AuthUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
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
    private final ApplicationEventPublisher applicationEventPublisher;
    private final AuthEmailBloomFilter authEmailBloomFilter;

    @Value("${auth.jwt.refresh-token-expiration-seconds:1209600}")
    private long refreshTokenExpirationSeconds;

    @Transactional(readOnly = true)
    public EmailDuplicateCheckResultDto checkEmailDuplicate(String email) {
        String normalizedEmail = normalizeEmail(email);
        /**
         * note -- 이메일 중복검사는 캐싱을 둘까? 얼마나?
         * 차피 index range scan 만 해서 ㅈㄴ 간단할거같은데 굳이 redis 들락날락거릴 필요 없을수도.
         */

        if (!authEmailBloomFilter.shouldCheckDb(normalizedEmail)) {
            return EmailDuplicateCheckResultDto.from(normalizedEmail, false);
        }

        boolean exists = authUserRepository.existsByEmail(normalizedEmail);

        return EmailDuplicateCheckResultDto.from(normalizedEmail, exists);
    }

    @Transactional
    public AuthTokenResponseDto signup(SignupRequestDto request) {
        String normalizedEmail = normalizeEmail(request.email());

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
        applicationEventPublisher.publishEvent(UserCreatedOutboxEvent.from(authUser));

        return issueTokens(authUser);
    }

    @Transactional
    public AuthTokenResponseDto login(LoginRequestDto request) {
        String normalizedEmail = normalizeEmail(request.email());

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

        return issueTokens(authUser);
    }

    @Transactional
    public AuthTokenResponseDto refresh(RefreshTokenRequestDto request) {
        RefreshToken refreshToken = refreshTokenRepository.findByTokenAndRevokedFalse(request.refreshToken())
            .orElseThrow(() -> new ApiException(
                "AUTH_INVALID_REFRESH_TOKEN",
                "Refresh token is invalid.",
                HttpStatus.UNAUTHORIZED
            ));

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
}
