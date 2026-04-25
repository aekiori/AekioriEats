package com.delivery.auth.service;

import com.delivery.auth.domain.token.RefreshToken;
import com.delivery.auth.dto.request.LoginRequestDto;
import com.delivery.auth.dto.request.LogoutRequestDto;
import com.delivery.auth.dto.request.RefreshTokenRequestDto;
import com.delivery.auth.dto.request.SignupRequestDto;
import com.delivery.auth.dto.response.AuthTokenResponseDto;
import com.delivery.auth.dto.response.EmailDuplicateCheckResponseDto;
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
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class AuthServiceIntegrationTest {
    private static final String TEST_IP = "127.0.0.1";

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
        jdbcTemplate.execute("delete from outbox");
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
        jdbcTemplate.execute("delete from outbox");
        jdbcTemplate.execute("delete from auth_users");
    }

    @Test
    void signup_creates_auth_user_and_user_created_outbox() throws Exception {
        AuthTokenResponseDto tokenResponse = authService.signup(
            new SignupRequestDto("new-user@example.com", "password1234"),
            TEST_IP
        );

        assertThat(tokenResponse.accessToken()).isNotBlank();
        assertThat(tokenResponse.refreshToken()).isNotBlank();

        Long userId = jdbcTemplate.queryForObject(
            "select user_id from auth_users where email = ?",
            Long.class,
            "new-user@example.com"
        );

        assertThat(userId).isNotNull();

        String status = jdbcTemplate.queryForObject(
            "select status from auth_users where user_id = ?",
            String.class,
            userId
        );
        String role = jdbcTemplate.queryForObject(
            "select role from auth_users where user_id = ?",
            String.class,
            userId
        );

        assertThat(status).isEqualTo("ACTIVE");
        assertThat(role).isEqualTo("USER");

        String outboxEventType = jdbcTemplate.queryForObject(
            "select event_type from outbox where aggregate_id = ?",
            String.class,
            userId
        );
        String outboxStatus = jdbcTemplate.queryForObject(
            "select status from outbox where aggregate_id = ?",
            String.class,
            userId
        );
        String partitionKey = jdbcTemplate.queryForObject(
            "select partition_key from outbox where aggregate_id = ?",
            String.class,
            userId
        );
        String payload = jdbcTemplate.queryForObject(
            "select payload from outbox where aggregate_id = ?",
            String.class,
            userId
        );

        assertThat(outboxEventType).isEqualTo("UserCreated");
        assertThat(outboxStatus).isEqualTo("INIT");
        assertThat(partitionKey).isEqualTo(String.valueOf(userId));
        assertThat(payload).contains("UserCreated");
        assertThat(payload).contains("new-user@example.com");

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode payloadNode = objectMapper.readTree(payload);
        if (payloadNode.isTextual()) {
            payloadNode = objectMapper.readTree(payloadNode.asText());
        }
        int schemaVersion = payloadNode.path("schemaVersion").isMissingNode()
            ? payloadNode.path("schema_version").asInt(-1)
            : payloadNode.path("schemaVersion").asInt(-1);
        boolean hasOccurredAt = payloadNode.hasNonNull("occurredAt") || payloadNode.hasNonNull("occurred_at");

        assertThat(schemaVersion).isEqualTo(1);
        assertThat(hasOccurredAt).isTrue();
    }

    @Test
    void signup_fails_when_email_is_duplicated() {
        authService.signup(new SignupRequestDto("dup-user@example.com", "password1234"), TEST_IP);

        assertThatThrownBy(() -> authService.signup(new SignupRequestDto("dup-user@example.com", "password1234"), TEST_IP))
            .isInstanceOf(ApiException.class)
            .hasMessageContaining("Email is already registered.");

        Long outboxCount = jdbcTemplate.queryForObject(
            "select count(*) from outbox where event_type = 'UserCreated'",
            Long.class
        );

        assertThat(outboxCount).isEqualTo(1L);
    }

    @Test
    void check_email_duplicate_returns_true_when_email_exists() {
        EmailDuplicateCheckResponseDto result = authService.checkEmailDuplicate("auth-user@example.com");

        assertThat(result.email()).isEqualTo("auth-user@example.com");
        assertThat(result.exists()).isTrue();
    }

    @Test
    void check_email_duplicate_returns_false_when_email_not_exists() {
        EmailDuplicateCheckResponseDto result = authService.checkEmailDuplicate("no-user@example.com");

        assertThat(result.email()).isEqualTo("no-user@example.com");
        assertThat(result.exists()).isFalse();
    }

    @Test
    void login_issues_access_and_refresh_tokens() {
        AuthTokenResponseDto tokenResponse = authService.login(
            new LoginRequestDto("auth-user@example.com", "password1234"),
            TEST_IP
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
        assertThatThrownBy(() -> authService.login(new LoginRequestDto("auth-user@example.com", "wrong-password"), TEST_IP))
            .isInstanceOf(ApiException.class)
            .hasMessageContaining("Email or password is invalid.");
    }

    @Test
    void refresh_rotates_refresh_token() {
        AuthTokenResponseDto first = authService.login(new LoginRequestDto("auth-user@example.com", "password1234"), TEST_IP);

        AuthTokenResponseDto second = authService.refresh(new RefreshTokenRequestDto(first.refreshToken()));

        assertThat(second.refreshToken()).isNotEqualTo(first.refreshToken());

        List<RefreshToken> activeTokens = refreshTokenRepository.findAll().stream()
            .filter(token -> !token.getRevoked())
            .toList();

        assertThat(activeTokens).hasSize(1);
        assertThat(activeTokens.get(0).getToken()).isEqualTo(second.refreshToken());
    }

    @Test
    void refresh_reuse_detection_revokes_all_active_tokens() {
        AuthTokenResponseDto first = authService.login(new LoginRequestDto("auth-user@example.com", "password1234"), TEST_IP);
        AuthTokenResponseDto second = authService.refresh(new RefreshTokenRequestDto(first.refreshToken()));

        assertThatThrownBy(() -> authService.refresh(new RefreshTokenRequestDto(first.refreshToken())))
            .isInstanceOf(ApiException.class)
            .satisfies(error -> {
                ApiException exception = (ApiException) error;
                assertThat(exception.getCode()).isEqualTo("AUTH_REFRESH_TOKEN_REUSE_DETECTED");
                assertThat(exception.getStatus()).isEqualTo(org.springframework.http.HttpStatus.UNAUTHORIZED);
            });

        assertThat(refreshTokenRepository.findByTokenAndRevokedFalse(second.refreshToken())).isEmpty();
    }

    @Test
    void logout_revokes_refresh_token() {
        AuthTokenResponseDto first = authService.login(new LoginRequestDto("auth-user@example.com", "password1234"), TEST_IP);
        authService.logout(new LogoutRequestDto(first.refreshToken()));

        assertThat(refreshTokenRepository.findByTokenAndRevokedFalse(first.refreshToken())).isEmpty();
    }
}
