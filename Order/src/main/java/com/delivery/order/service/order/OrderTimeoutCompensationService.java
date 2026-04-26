package com.delivery.order.service.order;

import com.delivery.order.constant.OrderStatusChangeReason;
import com.delivery.order.domain.order.Order;
import com.delivery.order.domain.order.OrderStatusHistory;
import com.delivery.order.repository.order.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderTimeoutCompensationService {
    private final OrderRepository orderRepository;
    private final RecordOrderStatusHistoryService recordOrderStatusHistoryService;

    @Value("${order.timeout-compensation.batch-size:100}")
    private int batchSize;

    @Value("${order.timeout-compensation.store-validation-timeout-minutes:10}")
    private long storeValidationTimeoutMinutes;

    @Value("${order.timeout-compensation.payment-result-timeout-minutes:10}")
    private long paymentResultTimeoutMinutes;

    @Value("${order.timeout-compensation.store-decision-timeout-minutes:15}")
    private long storeDecisionTimeoutMinutes;

    @Transactional
    public int compensateTimedOutOrders() {
        LocalDateTime now = LocalDateTime.now();

        int compensatedCount = 0;
        compensatedCount += compensate(
            Order.Status.PENDING,
            Order.Status.FAILED,
            now.minusMinutes(storeValidationTimeoutMinutes),
            OrderStatusChangeReason.STORE_VALIDATION_TIMEOUT
        );
        compensatedCount += compensate(
            Order.Status.PAYMENT_PENDING,
            Order.Status.FAILED,
            now.minusMinutes(paymentResultTimeoutMinutes),
            OrderStatusChangeReason.PAYMENT_RESULT_TIMEOUT
        );
        compensatedCount += compensate(
            Order.Status.PAID,
            Order.Status.REFUND_PENDING,
            now.minusMinutes(storeDecisionTimeoutMinutes),
            OrderStatusChangeReason.STORE_DECISION_TIMEOUT
        );

        if (compensatedCount > 0) {
            log.info("Timed out order compensation completed. compensatedCount={}", compensatedCount);
        }

        return compensatedCount;
    }

    private int compensate(
        Order.Status currentStatus,
        Order.Status targetStatus,
        LocalDateTime cutoff,
        String reason
    ) {
        List<Order> timedOutOrders = orderRepository.findTimedOutOrdersForUpdate(
            currentStatus,
            cutoff,
            PageRequest.of(0, batchSize)
        );

        for (Order order : timedOutOrders) {
            changeStatusByTimeout(order, targetStatus, reason);
        }

        return timedOutOrders.size();
    }

    private void changeStatusByTimeout(Order order, Order.Status targetStatus, String reason) {
        Order.Status currentStatus = order.getStatus();

        order.updateStatus(targetStatus, reason);
        Order savedOrder = orderRepository.save(order);
        recordOrderStatusHistoryService.record(
            savedOrder,
            currentStatus,
            targetStatus,
            reason,
            OrderStatusHistory.SourceType.TIMEOUT_COMPENSATION,
            null
        );

        log.warn(
            "Order status changed by timeout compensation. orderId={}, currentStatus={}, targetStatus={}, reason={}",
            savedOrder.getId(),
            currentStatus,
            targetStatus,
            reason
        );
    }
}
