package com.delivery.auth.service;

import com.delivery.auth.config.AuthRateLimitProperties;
import com.delivery.auth.exception.ApiException;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AuthRateLimitService {
    private final AuthRateLimitProperties properties;
    private final int cleanupThreshold;
    private final long maxWindowSeconds;
    private final Map<String, BucketHolder> buckets = new ConcurrentHashMap<>();

    public AuthRateLimitService(AuthRateLimitProperties properties) {
        this.properties = properties;
        this.cleanupThreshold = properties.cleanupThreshold();
        this.maxWindowSeconds = properties.maxWindowSeconds();
    }

    public void validateSignup(String email, String clientIp) {
        if (!properties.isEnabled()) {
            return;
        }

        AuthRateLimitProperties.Endpoint signup = properties.getSignup();
        requireBucket(
            "signup:ip:" + normalizeIp(clientIp),
            signup.getIp().requestLimit(),
            signup.getIp().rateLimitPeriodSeconds(),
            "Too many signup attempts. Please try again later."
        );
        requireBucket(
            "signup:account:" + normalizeEmail(email),
            signup.getAccount().requestLimit(),
            signup.getAccount().rateLimitPeriodSeconds(),
            "Too many signup attempts. Please try again later."
        );
    }

    public void validateLogin(String email, String clientIp) {
        if (!properties.isEnabled()) {
            return;
        }

        AuthRateLimitProperties.Endpoint login = properties.getLogin();
        requireBucket(
            "login:ip:" + normalizeIp(clientIp),
            login.getIp().requestLimit(),
            login.getIp().rateLimitPeriodSeconds(),
            "Too many login attempts. Please try again later."
        );
        requireBucket(
            "login:account:" + normalizeEmail(email),
            login.getAccount().requestLimit(),
            login.getAccount().rateLimitPeriodSeconds(),
            "Too many login attempts. Please try again later."
        );
    }

    public void clearLoginAccountLimit(String email) {
        if (!properties.isEnabled()) {
            return;
        }
        buckets.remove("login:account:" + normalizeEmail(email));
    }

    private void requireBucket(
        String bucketKey,
        int requestLimit,
        long rateLimitPeriodSeconds,
        String message
    ) {
        if (tryConsume(bucketKey, requestLimit, rateLimitPeriodSeconds)) {
            return;
        }

        throw new ApiException(
            "AUTH_RATE_LIMITED",
            message,
            HttpStatus.TOO_MANY_REQUESTS
        );
    }

    private boolean tryConsume(String bucketKey, int requestLimit, long rateLimitPeriodSeconds) {
        long now = Instant.now().getEpochSecond();
        maybeCleanup(now);

        BucketHolder bucketHolder = buckets.compute(bucketKey, (key, existing) -> {
            if (existing == null || existing.isConfigChanged(requestLimit, rateLimitPeriodSeconds)) {
                return BucketHolder.create(requestLimit, rateLimitPeriodSeconds, now);
            }

            existing.lastAccessEpochSecond = now;
            return existing;
        });

        return bucketHolder.bucket.tryConsume(1);
    }

    private void maybeCleanup(long now) {
        if (buckets.size() < cleanupThreshold) {
            return;
        }

        long expiredBefore = now - maxWindowSeconds;
        buckets.entrySet()
            .removeIf(entry -> entry.getValue().lastAccessEpochSecond < expiredBefore);
    }

    private String normalizeEmail(String email) {
        return email == null ? "unknown" : email.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeIp(String clientIp) {
        return clientIp == null ? "unknown" : clientIp.trim();
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

        private static BucketHolder create(
            int requestLimit,
            long rateLimitPeriodSeconds,
            long now
        ) {
            return new BucketHolder(
                newBucket(requestLimit, rateLimitPeriodSeconds),
                requestLimit,
                rateLimitPeriodSeconds,
                now
            );
        }

        private boolean isConfigChanged(int requestLimit, long rateLimitPeriodSeconds) {
            return this.requestLimit != requestLimit || this.rateLimitPeriodSeconds != rateLimitPeriodSeconds;
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
