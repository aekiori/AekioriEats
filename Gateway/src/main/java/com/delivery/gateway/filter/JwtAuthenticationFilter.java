package com.delivery.gateway.filter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {
    private static final String BEARER_PREFIX = "Bearer ";
    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();
    private static final List<String> PUBLIC_PATHS = List.of(
        "/actuator/**",
        "/api/v1/auth/**",
        "/api/v1/gateway/test/**"
    ); // todo -- 지금은 ㄱㅊ은데 확장 고려하면 하드코딩 말고 외부주입?

    private final SecretKey secretKey;
    private final JwtParser jwtParser;
    private final ObjectMapper objectMapper;

    public JwtAuthenticationFilter(
        @Value("${gateway.auth.jwt.secret}") String jwtSecret,
        ObjectMapper objectMapper
    ) {
        this.secretKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        this.jwtParser = Jwts.parser()
            .verifyWith(secretKey)
            .build();
        this.objectMapper = objectMapper;
    }

    @Override
    // todo -- 예외처리할때, 이게 만료된건지 없는건지 비정상적인건지 구별이 안된다.
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        /**
         * ServerHttpRequest cleanRequest = exchange.getRequest().mutate()
         *     .headers(h -> h.remove("X-User-Id"))
         *     .build();
         *
         * exchange = exchange.mutate().request(cleanRequest).build();
         */// 이거 없으면 외부에서 X-User-Id: 1 헤더 넣어서 관리자 계정 탈취 가능해요. 보안 취약점이에요.
        exchange = sanitizeIdentityHeaders(exchange);

        String path = exchange.getRequest().getPath().value();

        if (isPublicPath(path)) {
            return chain.filter(exchange);
        }

        String authorizationHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        if (authorizationHeader == null || !authorizationHeader.startsWith(BEARER_PREFIX)) {
            return unauthorized(
                exchange,
                path,
                "AUTH_MISSING_TOKEN",
                "Authorization header is missing or invalid."
            );
        }

        String accessToken = authorizationHeader.substring(BEARER_PREFIX.length()).trim();
        if (accessToken.isBlank()) {
            return unauthorized(
                exchange,
                path,
                "AUTH_INVALID_TOKEN",
                "Access token is invalid."
            );
        }

        try {
            Claims claims = jwtParser
                .parseSignedClaims(accessToken)
                .getPayload();

            String userId = claims.getSubject();
            String role = claims.get("role", String.class);

            if (userId == null || userId.isBlank()) {
                return unauthorized(
                    exchange,
                    path,
                    "AUTH_INVALID_TOKEN",
                    "Access token is invalid."
                );
            }

            ServerHttpRequest.Builder requestBuilder = exchange.getRequest().mutate()
                .header("X-User-Id", userId);

            if (role != null && !role.isBlank()) {
                requestBuilder.header("X-User-Role", role);
            }

            ServerHttpRequest requestWithUserId = requestBuilder.build();

            return chain.filter(exchange.mutate().request(requestWithUserId).build());
        } catch (ExpiredJwtException e) {
            log.info("JWT 만료. path={}, error={}", path, e.getMessage());
            return unauthorized(
                exchange,
                path,
                "AUTH_TOKEN_EXPIRED",
                "Access token has expired."
            );
        } catch (SignatureException | MalformedJwtException e) {
            log.warn("JWT 위변조/형식 오류. path={}, error={}", path, e.getMessage());
            return unauthorized(
                exchange,
                path,
                "AUTH_INVALID_TOKEN",
                "Access token is invalid."
            );
        } catch (UnsupportedJwtException e) {
            log.warn("지원하지 않는 JWT. path={}, error={}", path, e.getMessage());
            return unauthorized(
                exchange,
                path,
                "AUTH_UNSUPPORTED_TOKEN",
                "Access token is unsupported."
            );
        } catch (IllegalArgumentException e) {
            log.warn("JWT 파싱 인자 오류. path={}, error={}", path, e.getMessage());
            return unauthorized(
                exchange,
                path,
                "AUTH_INVALID_TOKEN",
                "Access token is invalid."
            );
        } catch (Exception e) {
            log.warn("JWT 검증 실패. path={}, error={}", path, e.getMessage());
            return unauthorized(
                exchange,
                path,
                "AUTH_TOKEN_VALIDATION_FAILED",
                "Token validation failed."
            );
        }
    }

    @Override
    public int getOrder() {
        return -100;
    }

    private boolean isPublicPath(String path) {
        // todo -- 매 요청마다 루프로 패스검사. 더 좋은방법 있는지 개선 필요
        return PUBLIC_PATHS.stream().anyMatch(pattern -> PATH_MATCHER.match(pattern, path));
    }

    private ServerWebExchange sanitizeIdentityHeaders(ServerWebExchange exchange) {
        ServerHttpRequest cleanRequest = exchange.getRequest().mutate()
            .headers(h -> {
                h.remove("X-User-Id");
                h.remove("X-User-Role");
            })
            .build();

        return exchange.mutate().request(cleanRequest).build();
    }

    private Mono<Void> unauthorized(
        ServerWebExchange exchange,
        String path,
        String code,
        String message
    ) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("timestamp", LocalDateTime.now().toString());
        payload.put("path", path);
        payload.put("code", code);
        payload.put("message", message);
        payload.put("errors", null);

        try {
            byte[] bytes = objectMapper.writeValueAsBytes(payload);
            return exchange.getResponse()
                .writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(bytes)));
        } catch (JsonProcessingException e) {
            log.error("401 응답 직렬화 실패. path={}, code={}", path, code, e);
            return exchange.getResponse().setComplete();
        }
    }
}
