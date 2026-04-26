package com.delivery.order.service.order;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
@Profile("!test")
@RequiredArgsConstructor
@Slf4j
public class OrderTimeoutCompensationScheduler {
    private final OrderTimeoutCompensationService orderTimeoutCompensationService;
    private final RedissonClient redissonClient;

    @Value("${order.timeout-compensation.enabled:true}")
    private boolean enabled;

    @Value("${order.timeout-compensation.lock-name:order:timeout-compensation}")
    private String lockName;

    @Value("${order.timeout-compensation.lock-wait-ms:0}")
    private long lockWaitMs;

    @Value("${order.timeout-compensation.lock-lease-ms:60000}")
    private long lockLeaseMs;

    @Scheduled(
        fixedDelayString = "${order.timeout-compensation.fixed-delay-ms:60000}",
        initialDelayString = "${order.timeout-compensation.initial-delay-ms:30000}"
    )
    public void compensateTimedOutOrders() {
        if (!enabled) {
            return;
        }

        RLock lock = redissonClient.getLock(lockName);
        boolean acquired = false;

        try {
            acquired = lock.tryLock(lockWaitMs, lockLeaseMs, TimeUnit.MILLISECONDS);
            if (!acquired) {
                log.debug("Order timeout compensation skipped because lock was not acquired. lockName={}", lockName);
                return;
            }

            orderTimeoutCompensationService.compensateTimedOutOrders();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            log.warn("Order timeout compensation interrupted while acquiring lock. lockName={}", lockName, exception);
        } finally {
            if (acquired && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
