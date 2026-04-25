package com.delivery.order.service.idempotency;

import com.delivery.order.dto.response.CreateOrderResponseDto;
import com.delivery.order.service.idempotency.OrderIdempotencyCacheService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;

@Component
@Profile("!test")
@RequiredArgsConstructor
public class RedisOrderIdempotencyCacheService implements OrderIdempotencyCacheService {
    private static final Logger log = LoggerFactory.getLogger(RedisOrderIdempotencyCacheService.class);

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${order.idempotency.lock-ttl-seconds:30}")
    private long lockTtlSeconds;

    @Value("${order.idempotency.result-ttl-seconds:600}")
    private long resultTtlSeconds;

    @Override
    public CreateOrderResponseDto getCompletedResult(String idempotencyKey) {
        String cachedValue = stringRedisTemplate.opsForValue().get(resultKey(idempotencyKey));

        if (cachedValue == null || cachedValue.isBlank()) {
            return null;
        }

        try {
            return objectMapper.readValue(cachedValue, CreateOrderResponseDto.class);
        } catch (Exception exception) {
            log.warn("Redis 멱등성 결과 파싱 실패. idempotencyKey={}", idempotencyKey, exception);
            stringRedisTemplate.delete(resultKey(idempotencyKey));
            return null;
        }
    }

    @Override
    public String getProcessingRequestHash(String idempotencyKey) {
        return stringRedisTemplate.opsForValue().get(lockKey(idempotencyKey));
    }

    @Override
    public boolean tryAcquire(String idempotencyKey, String requestHash) {
        Boolean acquired = stringRedisTemplate.opsForValue().setIfAbsent(
            lockKey(idempotencyKey),
            requestHash,
            Duration.ofSeconds(lockTtlSeconds)
        );

        return Boolean.TRUE.equals(acquired);
    }

    @Override
    public void saveCompletedResult(String idempotencyKey, CreateOrderResponseDto result) {
        try {
            String value = objectMapper.writeValueAsString(result);

            stringRedisTemplate.opsForValue().set(
                resultKey(idempotencyKey),
                value,
                Duration.ofSeconds(resultTtlSeconds)
            );
            stringRedisTemplate.delete(lockKey(idempotencyKey));
        } catch (Exception exception) {
            log.warn("Redis 멱등성 결과 저장 실패. idempotencyKey={}", idempotencyKey, exception);
        }
    }

    @Override
    public void release(String idempotencyKey) {
        stringRedisTemplate.delete(lockKey(idempotencyKey));
    }

    private String lockKey(String idempotencyKey) {
        return "order:idempotency:lock:" + idempotencyKey;
    }

    private String resultKey(String idempotencyKey) {
        return "order:idempotency:result:" + idempotencyKey;
    }
}

