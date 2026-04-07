package com.delivery.gateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {
    private static final String BEARER_PREFIX = "Bearer ";
    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();
    private static final List<String> PUBLIC_PATHS = List.of(
        "/actuator/**",
        "/api/v1/auth/**"
    ); // todo -- 지금은 ㄱㅊ은데 확장 고려하면 하드코딩 말고 외부주입 고려 필요

    private final SecretKey secretKey;
    private final JwtParser jwtParser;

    public JwtAuthenticationFilter(@Value("${gateway.auth.jwt.secret}") String jwtSecret) {
        this.secretKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        this.jwtParser = Jwts.parser()
            .verifyWith(secretKey)
            .build();
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

        String path = exchange.getRequest().getPath().value();

        if (isPublicPath(path)) {
            return chain.filter(exchange);
        }

        String authorizationHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        if (authorizationHeader == null || !authorizationHeader.startsWith(BEARER_PREFIX)) {
            return unauthorized(exchange);
        }

        // String accessToken = authorizationHeader.substring(BEARER_PREFIX.length()).trim();
        String accessToken = authorizationHeader.substring(BEARER_PREFIX.length());

        try {
            Claims claims = jwtParser
                .parseSignedClaims(accessToken)
                .getPayload();

            String userId = claims.getSubject();
            String role = claims.get("role", String.class);

            if (userId == null || userId.isBlank()) {
                return unauthorized(exchange);
            }

            ServerHttpRequest.Builder requestBuilder = exchange.getRequest().mutate()
                .header("X-User-Id", userId);

            if (role != null && !role.isBlank()) {
                requestBuilder.header("X-User-Role", role);
            }

            ServerHttpRequest requestWithUserId = requestBuilder.build();

            return chain.filter(exchange.mutate().request(requestWithUserId).build());
        } catch (Exception e) {
            log.warn("JWT 검증 실패. path={}, error={}", path, e.getMessage());
            return unauthorized(exchange);
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

    private Mono<Void> unauthorized(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);

        return exchange.getResponse().setComplete();
    }
}
