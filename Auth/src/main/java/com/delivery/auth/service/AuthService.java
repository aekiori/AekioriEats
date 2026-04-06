package com.delivery.auth.service;

import com.delivery.auth.domain.token.RefreshToken;
import com.delivery.auth.domain.user.AuthUser;
import com.delivery.auth.dto.request.LoginRequestDto;
import com.delivery.auth.dto.request.LogoutRequestDto;
import com.delivery.auth.dto.request.RefreshTokenRequestDto;
import com.delivery.auth.dto.response.AuthTokenResponseDto;
import com.delivery.auth.exception.ApiException;
import com.delivery.auth.repository.token.RefreshTokenRepository;
import com.delivery.auth.repository.user.AuthUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
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

    @Value("${auth.jwt.refresh-token-expiration-seconds:1209600}")
    private long refreshTokenExpirationSeconds;

    @Transactional
    public AuthTokenResponseDto login(LoginRequestDto request) {
        String normalizedEmail = request.email().toLowerCase(Locale.ROOT);

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
}
