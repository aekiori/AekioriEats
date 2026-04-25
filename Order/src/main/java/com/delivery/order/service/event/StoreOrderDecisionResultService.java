package com.delivery.order.service.event;

import com.delivery.order.constant.OrderEventType;
import com.delivery.order.constant.OrderStatusChangeReason;
import com.delivery.order.domain.order.Order;
import com.delivery.order.domain.order.OrderStatusHistory;
import com.delivery.order.dto.event.StoreOrderDecisionEventDto;
import com.delivery.order.repository.order.OrderRepository;
import com.delivery.order.service.order.RecordOrderStatusHistoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class StoreOrderDecisionResultService {
    private final OrderRepository orderRepository;
    private final RecordOrderStatusHistoryService recordOrderStatusHistoryService;

    @Transactional
    public void handle(StoreOrderDecisionEventDto event) {
        Order order = orderRepository.findById(event.orderId()).orElse(null);
        if (order == null) {
            log.warn(
                "Store order decision ignored because order was not found. eventType={}, eventId={}, orderId={}",
                event.eventType(),
                event.eventId(),
                event.orderId()
            );
            return;
        }

        if (!order.getStoreId().equals(event.storeId())) {
            log.warn(
                "Store order decision ignored because storeId mismatched. eventType={}, eventId={}, orderId={}, eventStoreId={}, orderStoreId={}",
                event.eventType(),
                event.eventId(),
                event.orderId(),
                event.storeId(),
                order.getStoreId()
            );
            return;
        }

        if (OrderEventType.STORE_ORDER_ACCEPTED.equals(event.eventType())) {
            handleAccepted(order, event);
            return;
        }

        if (OrderEventType.STORE_ORDER_REJECTED.equals(event.eventType())) {
            handleRejected(order, event);
        }
    }

    private void handleAccepted(Order order, StoreOrderDecisionEventDto event) {
        if (order.getStatus() == Order.Status.ACCEPTED) {
            log.debug("Order already accepted. eventId={}, orderId={}", event.eventId(), event.orderId());
            return;
        }

        if (order.getStatus() != Order.Status.PAID) {
            log.warn(
                "Store order accepted event skipped because status is not PAID. eventId={}, orderId={}, currentStatus={}",
                event.eventId(),
                event.orderId(),
                order.getStatus()
            );
            return;
        }

        String reason = OrderStatusChangeReason.STORE_ACCEPTED_PAID_ORDER;
        changeStatusByStoreDecisionEvent(order, Order.Status.ACCEPTED, reason, event.eventId());
        log.info("Order status changed by store decision. eventId={}, orderId={}, status=PAID->ACCEPTED", event.eventId(), event.orderId());
    }

    private void handleRejected(Order order, StoreOrderDecisionEventDto event) {
        if (order.getStatus() == Order.Status.REFUND_PENDING) {
            log.debug("Order already refund pending. eventId={}, orderId={}", event.eventId(), event.orderId());
            return;
        }

        if (order.getStatus() != Order.Status.PAID) {
            log.warn(
                "Store order rejected event skipped because status is not PAID. eventId={}, orderId={}, currentStatus={}",
                event.eventId(),
                event.orderId(),
                order.getStatus()
            );
            return;
        }

        String reason = buildRejectedReason(event);
        changeStatusByStoreDecisionEvent(order, Order.Status.REFUND_PENDING, reason, event.eventId());
        log.info("Order status changed by store decision. eventId={}, orderId={}, status=PAID->REFUND_PENDING", event.eventId(), event.orderId());
    }

    private void changeStatusByStoreDecisionEvent(
        Order order,
        Order.Status targetStatus,
        String reason,
        String eventId
    ) {
        Order.Status currentStatus = order.getStatus();
        order.updateStatus(targetStatus, reason);
        Order savedOrder = orderRepository.save(order);
        recordOrderStatusHistoryService.record(
            savedOrder,
            currentStatus,
            targetStatus,
            reason,
            OrderStatusHistory.SourceType.STORE_DECISION_EVENT,
            eventId
        );
    }

    private String buildRejectedReason(StoreOrderDecisionEventDto event) {
        if (event.rejectReason() == null || event.rejectReason().isBlank()) {
            return OrderStatusChangeReason.STORE_REJECTED_PAID_ORDER;
        }
        return event.rejectReason();
    }
}

