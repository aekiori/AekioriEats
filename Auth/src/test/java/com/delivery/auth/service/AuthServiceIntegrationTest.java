package com.delivery.auth.service;

import com.delivery.auth.domain.token.RefreshToken;
import com.delivery.auth.dto.request.LoginRequestDto;
import com.delivery.auth.dto.request.LogoutRequestDto;
import com.delivery.auth.dto.request.RefreshTokenRequestDto;
import com.delivery.auth.dto.response.AuthTokenResponseDto;
import com.delivery.auth.exception.ApiException;
import com.delivery.auth.repository.token.RefreshTokenRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class AuthServiceIntegrationTest {
    @Autowired
    private AuthService authService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("delete from auth_users");
        refreshTokenRepository.deleteAll();

        jdbcTemplate.update(
            "insert into auth_users(user_id, email, nickname, role, password_hash, status, created_at, updated_at) values (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP())",
            1L,
            "auth-user@example.com",
            "pink",
            "USER",
            passwordEncoder.encode("password1234"),
            "ACTIVE"
        );
    }

    @AfterEach
    void tearDown() {
        refreshTokenRepository.deleteAll();
        jdbcTemplate.execute("delete from auth_users");
    }

    @Test
    void login_issues_access_and_refresh_tokens() {
        AuthTokenResponseDto tokenResponse = authService.login(
            new LoginRequestDto("auth-user@example.com", "password1234")
        );

        assertThat(tokenResponse.accessToken()).isNotBlank();
        assertThat(tokenResponse.refreshToken()).isNotBlank();
        assertThat(tokenResponse.tokenType()).isEqualTo("Bearer");

        Claims claims = Jwts.parser()
            .verifyWith(Keys.hmacShaKeyFor("auth-test-secret-auth-test-secret-2026".getBytes(StandardCharsets.UTF_8)))
            .build()
            .parseSignedClaims(tokenResponse.accessToken())
            .getPayload();

        assertThat(claims.getSubject()).isEqualTo("1");
        assertThat(((Number) claims.get("user_id")).longValue()).isEqualTo(1L);
        assertThat(claims.get("email", String.class)).isEqualTo("auth-user@example.com");
        assertThat(claims.get("nickname", String.class)).isEqualTo("pink");
        assertThat(claims.get("role", String.class)).isEqualTo("USER");
        assertThat(claims.get("type", String.class)).isEqualTo("access");
        assertThat(claims.getIssuer()).isEqualTo("aekiori-eats");
        assertThat(claims.getId()).isNotBlank();
    }

    @Test
    void login_fails_when_password_is_invalid() {
        assertThatThrownBy(() -> authService.login(new LoginRequestDto("auth-user@example.com", "wrong-password")))
            .isInstanceOf(ApiException.class)
            .hasMessageContaining("Email or password is invalid.");
    }

    @Test
    void refresh_rotates_refresh_token() {
        AuthTokenResponseDto first = authService.login(new LoginRequestDto("auth-user@example.com", "password1234"));

        AuthTokenResponseDto second = authService.refresh(new RefreshTokenRequestDto(first.refreshToken()));

        assertThat(second.refreshToken()).isNotEqualTo(first.refreshToken());

        List<RefreshToken> activeTokens = refreshTokenRepository.findAll().stream()
            .filter(token -> !token.getRevoked())
            .toList();

        assertThat(activeTokens).hasSize(1);
        assertThat(activeTokens.get(0).getToken()).isEqualTo(second.refreshToken());
    }

    @Test
    void logout_revokes_refresh_token() {
        AuthTokenResponseDto first = authService.login(new LoginRequestDto("auth-user@example.com", "password1234"));
        authService.logout(new LogoutRequestDto(first.refreshToken()));

        assertThat(refreshTokenRepository.findByTokenAndRevokedFalse(first.refreshToken())).isEmpty();
    }
}
