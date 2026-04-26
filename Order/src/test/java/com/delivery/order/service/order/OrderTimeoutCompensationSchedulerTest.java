package com.delivery.order.service.order;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.TimeUnit;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderTimeoutCompensationSchedulerTest {
    @Mock
    private OrderTimeoutCompensationService orderTimeoutCompensationService;

    @Mock
    private RedissonClient redissonClient;

    @Mock
    private RLock lock;

    private OrderTimeoutCompensationScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new OrderTimeoutCompensationScheduler(orderTimeoutCompensationService, redissonClient);
        ReflectionTestUtils.setField(scheduler, "enabled", true);
        ReflectionTestUtils.setField(scheduler, "lockName", "order:timeout-compensation");
        ReflectionTestUtils.setField(scheduler, "lockWaitMs", 0L);
        ReflectionTestUtils.setField(scheduler, "lockLeaseMs", 60000L);
    }

    @Test
    void runs_compensation_when_lock_acquired() throws Exception {
        when(redissonClient.getLock("order:timeout-compensation")).thenReturn(lock);
        when(lock.tryLock(0L, 60000L, TimeUnit.MILLISECONDS)).thenReturn(true);
        when(lock.isHeldByCurrentThread()).thenReturn(true);

        scheduler.compensateTimedOutOrders();

        verify(orderTimeoutCompensationService).compensateTimedOutOrders();
        verify(lock).unlock();
    }

    @Test
    void skips_compensation_when_lock_not_acquired() throws Exception {
        when(redissonClient.getLock("order:timeout-compensation")).thenReturn(lock);
        when(lock.tryLock(0L, 60000L, TimeUnit.MILLISECONDS)).thenReturn(false);

        scheduler.compensateTimedOutOrders();

        verify(orderTimeoutCompensationService, never()).compensateTimedOutOrders();
        verify(lock, never()).unlock();
    }
}
