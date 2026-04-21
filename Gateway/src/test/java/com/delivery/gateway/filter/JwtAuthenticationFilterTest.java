package com.delivery.gateway.filter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class JwtAuthenticationFilterTest {
    private static final String JWT_SECRET = "auth-super-secret-auth-super-secret-2026";
    private static final String OTHER_SECRET = "another-super-secret-another-super-secret-2026";

    private JwtAuthenticationFilter jwtAuthenticationFilter;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        jwtAuthenticationFilter = new JwtAuthenticationFilter(JWT_SECRET, objectMapper);
    }

    @Test
    void filter_strips_forged_identity_headers_and_injects_claim_values() {
        String token = createToken(JWT_SECRET, "42", "USER", Instant.now().plusSeconds(300));
        MockServerWebExchange exchange = MockServerWebExchange.from(
            MockServerHttpRequest.get("/api/v1/orders/1")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .header("X-User-Id", "999")
                .header("X-User-Role", "FORGED")
                .build()
        );
        CapturingChain chain = new CapturingChain();

        StepVerifier.create(jwtAuthenticationFilter.filter(exchange, chain))
            .verifyComplete();

        assertThat(chain.invoked.get()).isTrue();
        assertThat(chain.exchangeRef.get()).isNotNull();
        assertThat(chain.exchangeRef.get().getRequest().getHeaders().getFirst("X-User-Id")).isEqualTo("42");
        assertThat(chain.exchangeRef.get().getRequest().getHeaders().getFirst("X-User-Role")).isEqualTo("USER");
    }

    @Test
    void filter_accepts_bearer_with_extra_spaces_before_token() {
        String token = createToken(JWT_SECRET, "77", "USER", Instant.now().plusSeconds(300));
        MockServerWebExchange exchange = MockServerWebExchange.from(
            MockServerHttpRequest.get("/api/v1/users/77")
                .header(HttpHeaders.AUTHORIZATION, "Bearer  " + token)
                .build()
        );
        CapturingChain chain = new CapturingChain();

        StepVerifier.create(jwtAuthenticationFilter.filter(exchange, chain))
            .verifyComplete();

        assertThat(chain.invoked.get()).isTrue();
        assertThat(chain.exchangeRef.get().getRequest().getHeaders().getFirst("X-User-Id")).isEqualTo("77");
    }

    @Test
    void filter_returns_auth_invalid_token_for_signature_mismatch() throws Exception {
        String forgedToken = createToken(OTHER_SECRET, "12", "USER", Instant.now().plusSeconds(300));
        MockServerWebExchange exchange = MockServerWebExchange.from(
            MockServerHttpRequest.get("/api/v1/orders/1")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + forgedToken)
                .build()
        );
        CapturingChain chain = new CapturingChain();

        StepVerifier.create(jwtAuthenticationFilter.filter(exchange, chain))
            .verifyComplete();

        assertThat(chain.invoked.get()).isFalse();
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        String body = exchange.getResponse().getBodyAsString().block();
        JsonNode json = objectMapper.readTree(body);
        assertThat(json.path("code").asText()).isEqualTo("AUTH_INVALID_TOKEN");
    }

    @Test
    void filter_returns_auth_token_expired_for_expired_token() throws Exception {
        String expiredToken = createToken(JWT_SECRET, "15", "USER", Instant.now().minusSeconds(30));
        MockServerWebExchange exchange = MockServerWebExchange.from(
            MockServerHttpRequest.get("/api/v1/orders/1")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + expiredToken)
                .build()
        );
        CapturingChain chain = new CapturingChain();

        StepVerifier.create(jwtAuthenticationFilter.filter(exchange, chain))
            .verifyComplete();

        assertThat(chain.invoked.get()).isFalse();
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        String body = exchange.getResponse().getBodyAsString().block();
        JsonNode json = objectMapper.readTree(body);
        assertThat(json.path("code").asText()).isEqualTo("AUTH_TOKEN_EXPIRED");
    }

    private String createToken(String secret, String subject, String role, Instant expiresAt) {
        Instant issuedAt = expiresAt.minusSeconds(60);
        return Jwts.builder()
            .subject(subject)
            .claim("role", role)
            .issuedAt(Date.from(issuedAt))
            .expiration(Date.from(expiresAt))
            .signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)))
            .compact();
    }

    private static class CapturingChain implements GatewayFilterChain {
        private final AtomicBoolean invoked = new AtomicBoolean(false);
        private final AtomicReference<ServerWebExchange> exchangeRef = new AtomicReference<>();

        @Override
        public Mono<Void> filter(ServerWebExchange exchange) {
            invoked.set(true);
            exchangeRef.set(exchange);
            return Mono.empty();
        }
    }
}

