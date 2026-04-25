package com.delivery.order.service.event;

import com.delivery.order.constant.OrderEventType;
import com.delivery.order.constant.OrderStatusChangeReason;
import com.delivery.order.domain.order.Order;
import com.delivery.order.domain.order.OrderStatusHistory;
import com.delivery.order.dto.event.PaymentResultEventDto;
import com.delivery.order.repository.order.OrderRepository;
import com.delivery.order.service.order.RecordOrderStatusHistoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentResultService {
    private final OrderRepository orderRepository;
    private final RecordOrderStatusHistoryService recordOrderStatusHistoryService;

    @Transactional
    public void handle(PaymentResultEventDto event) {
        Order order = orderRepository.findById(event.orderId()).orElse(null);

        if (order == null) {
            log.warn(
                "Payment result event ignored because order was not found. eventType={}, eventId={}, orderId={}",
                event.eventType(),
                event.eventId(),
                event.orderId()
            );
            return;
        }

        if (OrderEventType.PAYMENT_SUCCEEDED.equals(event.eventType())) {
            handleSucceeded(order, event);
            return;
        }

        if (OrderEventType.PAYMENT_FAILED.equals(event.eventType())) {
            handleFailed(order, event);
            return;
        }

        if (OrderEventType.PAYMENT_REFUNDED.equals(event.eventType())) {
            handleRefunded(order, event);
        }
    }

    private void handleSucceeded(Order order, PaymentResultEventDto event) {
        if (order.getStatus() == Order.Status.PAID) {
            log.debug(
                "Order already paid. eventId={}, orderId={}, paymentId={}",
                event.eventId(),
                event.orderId(),
                event.paymentId()
            );
            return;
        }

        if (order.getStatus() != Order.Status.PAYMENT_PENDING) {
            log.warn(
                "Payment succeeded event skipped because status is not PAYMENT_PENDING. eventId={}, orderId={}, currentStatus={}",
                event.eventId(),
                event.orderId(),
                order.getStatus()
            );
            return;
        }

        String reason = OrderStatusChangeReason.PAYMENT_SUCCEEDED;
        changeStatusByPaymentEvent(order, Order.Status.PAID, reason, event.eventId());

        log.info(
            "Order status changed by payment succeeded. eventId={}, orderId={}, paymentId={}, status=PAYMENT_PENDING->PAID",
            event.eventId(),
            event.orderId(),
            event.paymentId()
        );
    }

    private void handleFailed(Order order, PaymentResultEventDto event) {
        if (order.getStatus() == Order.Status.FAILED || order.getStatus() == Order.Status.CANCELLED) {
            log.debug(
                "Order already failed or cancelled. eventId={}, orderId={}, currentStatus={}",
                event.eventId(),
                event.orderId(),
                order.getStatus()
            );
            return;
        }

        if (order.getStatus() != Order.Status.PAYMENT_PENDING) {
            log.warn(
                "Payment failed event skipped because status is not PAYMENT_PENDING. eventId={}, orderId={}, currentStatus={}",
                event.eventId(),
                event.orderId(),
                order.getStatus()
            );
            return;
        }

        String reason = buildFailedReason(event);
        changeStatusByPaymentEvent(order, Order.Status.FAILED, reason, event.eventId());

        log.info(
            "Order status changed by payment failed. eventId={}, orderId={}, paymentId={}, status=PAYMENT_PENDING->FAILED",
            event.eventId(),
            event.orderId(),
            event.paymentId()
        );
    }

    private String buildFailedReason(PaymentResultEventDto event) {
        if (event.failReason() == null || event.failReason().isBlank()) {
            return OrderStatusChangeReason.PAYMENT_FAILED;
        }

        return event.failReason();
    }

    private void handleRefunded(Order order, PaymentResultEventDto event) {
        if (order.getStatus() == Order.Status.REFUNDED) {
            log.debug(
                "Order already refunded. eventId={}, orderId={}, paymentId={}",
                event.eventId(),
                event.orderId(),
                event.paymentId()
            );
            return;
        }

        if (order.getStatus() != Order.Status.REFUND_PENDING) {
            log.warn(
                "Payment refunded event skipped because status is not REFUND_PENDING. eventId={}, orderId={}, currentStatus={}",
                event.eventId(),
                event.orderId(),
                order.getStatus()
            );
            return;
        }

        String reason = buildRefundReason(event);
        changeStatusByPaymentEvent(order, Order.Status.REFUNDED, reason, event.eventId());

        log.info(
            "Order status changed by payment refunded. eventId={}, orderId={}, paymentId={}, status=REFUND_PENDING->REFUNDED",
            event.eventId(),
            event.orderId(),
            event.paymentId()
        );
    }

    private void changeStatusByPaymentEvent(
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
            OrderStatusHistory.SourceType.PAYMENT_EVENT,
            eventId
        );
    }

    private String buildRefundReason(PaymentResultEventDto event) {
        if (event.failReason() == null || event.failReason().isBlank()) {
            return OrderStatusChangeReason.PAYMENT_REFUNDED;
        }

        return event.failReason();
    }
}

