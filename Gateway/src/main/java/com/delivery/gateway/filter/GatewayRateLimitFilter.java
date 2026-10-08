package com.delivery.gateway.filter;

import com.delivery.gateway.config.GatewayRateLimitProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class GatewayRateLimitFilter implements GlobalFilter, Ordered {
    private static final String UNKNOWN_CLIENT = "unknown";
    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    private final GatewayRateLimitProperties properties;
    private final ObjectMapper objectMapper;
    private final Map<String, BucketHolder> buckets = new ConcurrentHashMap<>();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (!properties.isEnabled()) {
            return chain.filter(exchange);
        }

        String path = exchange.getRequest().getPath().value();
        GatewayRateLimitProperties.Route matchedRoute = findRoute(path);
        if (matchedRoute == null) {
            return chain.filter(exchange);
        }

        String clientIp = resolveClientIp(exchange);
        String routeKey = String.join(",", matchedRoute.getPathPatterns());
        String bucketKey = "route:" + routeKey + ":ip:" + clientIp;

        if (tryConsume(bucketKey, matchedRoute)) {
            return chain.filter(exchange);
        }

        log.warn("Gateway rate limit exceeded. path={}, clientIp={}", path, clientIp);
        return tooManyRequests(exchange, path, matchedRoute.rateLimitPeriodSeconds());
    }

    @Override
    public int getOrder() {
        return -200;
    }

    private GatewayRateLimitProperties.Route findRoute(String path) {
        return properties.enabledRoutes().stream()
            .filter(route -> route.getPathPatterns().stream().anyMatch(pattern -> PATH_MATCHER.match(pattern, path)))
            .findFirst()
            .orElse(null);
    }

    private boolean tryConsume(String bucketKey, GatewayRateLimitProperties.Route route) {
        long now = Instant.now().getEpochSecond();
        maybeCleanup(now);

        BucketHolder holder = buckets.compute(bucketKey, (key, existing) -> {
            if (existing == null || existing.isConfigChanged(route)) {
                return BucketHolder.create(route, now);
            }

            existing.lastAccessEpochSecond = now;
            return existing;
        });

        return holder.bucket.tryConsume(1);
    }

    private void maybeCleanup(long now) {
        if (buckets.size() < properties.cleanupThreshold()) {
            return;
        }

        long expiredBefore = now - properties.maxWindowSeconds();
        buckets.entrySet().removeIf(entry -> entry.getValue().lastAccessEpochSecond < expiredBefore);
    }

    private String resolveClientIp(ServerWebExchange exchange) {
        if (properties.isTrustForwardedFor()) {
            String forwardedFor = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
            if (forwardedFor != null && !forwardedFor.isBlank()) {
                return forwardedFor.split(",")[0].trim();
            }
        }

        InetSocketAddress remoteAddress = exchange.getRequest().getRemoteAddress();
        if (remoteAddress == null) {
            return UNKNOWN_CLIENT;
        }
        if (remoteAddress.getAddress() != null) {
            return remoteAddress.getAddress().getHostAddress();
        }
        return remoteAddress.getHostString();
    }

    private Mono<Void> tooManyRequests(ServerWebExchange exchange, String path, long retryAfterSeconds) {
        exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        exchange.getResponse().getHeaders().set(HttpHeaders.RETRY_AFTER, String.valueOf(retryAfterSeconds));

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("timestamp", LocalDateTime.now().toString());
        payload.put("path", path);
        payload.put("traceId", GatewayTraceIdFilter.getTraceId(exchange));
        payload.put("code", "RATE_LIMITED");
        payload.put("message", "Too many requests. Please try again later.");
        payload.put("errors", null);

        try {
            byte[] bytes = objectMapper.writeValueAsBytes(payload);
            return exchange.getResponse()
                .writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(bytes)));
        } catch (JsonProcessingException exception) {
            log.error("Failed to serialize 429 response. path={}", path, exception);
            return exchange.getResponse().setComplete();
        }
    }

    private static final class BucketHolder {
        private final Bucket bucket;
        private final int requestLimit;
        private final long rateLimitPeriodSeconds;
        private volatile long lastAccessEpochSecond;

        private BucketHolder(
            Bucket bucket,
            int requestLimit,
            long rateLimitPeriodSeconds,
            long lastAccessEpochSecond
        ) {
            this.bucket = bucket;
            this.requestLimit = requestLimit;
            this.rateLimitPeriodSeconds = rateLimitPeriodSeconds;
            this.lastAccessEpochSecond = lastAccessEpochSecond;
        }

        private static BucketHolder create(GatewayRateLimitProperties.Route route, long now) {
            return new BucketHolder(
                newBucket(route.requestLimit(), route.rateLimitPeriodSeconds()),
                route.requestLimit(),
                route.rateLimitPeriodSeconds(),
                now
            );
        }

        private boolean isConfigChanged(GatewayRateLimitProperties.Route route) {
            return requestLimit != route.requestLimit()
                || rateLimitPeriodSeconds != route.rateLimitPeriodSeconds();
        }

        private static Bucket newBucket(int requestLimit, long rateLimitPeriodSeconds) {
            Bandwidth limit = Bandwidth.classic(
                requestLimit,
                Refill.greedy(requestLimit, Duration.ofSeconds(rateLimitPeriodSeconds))
            );
            return Bucket.builder()
                .addLimit(limit)
                .build();
        }
    }
}
