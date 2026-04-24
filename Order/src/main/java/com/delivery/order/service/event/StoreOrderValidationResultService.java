package com.delivery.order.service.event;

import com.delivery.order.constant.OrderEventType;
import com.delivery.order.constant.OrderStatusChangeReason;
import com.delivery.order.domain.order.Order;
import com.delivery.order.domain.order.OrderStatusHistory;
import com.delivery.order.dto.event.StoreOrderValidationEventDto;
import com.delivery.order.repository.order.OrderRepository;
import com.delivery.order.service.order.RecordOrderStatusHistoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class StoreOrderValidationResultService {
    private final OrderRepository orderRepository;
    private final RecordOrderStatusHistoryService recordOrderStatusHistoryService;

    @Transactional
    public void handle(StoreOrderValidationEventDto event) {
        Order order = orderRepository.findById(event.orderId()).orElse(null);

        if (order == null) {
            log.warn(
                "Store validation event ignored because order was not found. eventType={}, eventId={}, orderId={}",
                event.eventType(),
                event.eventId(),
                event.orderId()
            );
            return;
        }

        if (!order.getStoreId().equals(event.storeId())) {
            log.warn(
                "Store validation event ignored because storeId mismatched. eventType={}, eventId={}, orderId={}, eventStoreId={}, orderStoreId={}",
                event.eventType(),
                event.eventId(),
                event.orderId(),
                event.storeId(),
                order.getStoreId()
            );
            return;
        }

        if (OrderEventType.ORDER_VALIDATED.equals(event.eventType())) {
            handleValidated(order, event);
            return;
        }

        if (OrderEventType.ORDER_REJECTED.equals(event.eventType())) {
            handleRejected(order, event);
        }
    }

    private void handleValidated(Order order, StoreOrderValidationEventDto event) {
        if (order.getStatus() == Order.Status.PAYMENT_PENDING || order.getStatus() == Order.Status.PAID) {
            log.debug(
                "Order already passed store validation. eventId={}, orderId={}, currentStatus={}",
                event.eventId(),
                event.orderId(),
                order.getStatus()
            );
            return;
        }

        if (order.getStatus() != Order.Status.PENDING) {
            log.warn(
                "Store validated event skipped because status is not PENDING. eventId={}, orderId={}, currentStatus={}",
                event.eventId(),
                event.orderId(),
                order.getStatus()
            );
            return;
        }

        Order.Status currentStatus = order.getStatus();
        String reason = OrderStatusChangeReason.STORE_VALIDATION_PASSED;
        order.updateStatus(Order.Status.PAYMENT_PENDING, reason);
        Order savedOrder = orderRepository.save(order);
        recordOrderStatusHistoryService.record(
            savedOrder,
            currentStatus,
            Order.Status.PAYMENT_PENDING,
            reason,
            OrderStatusHistory.SourceType.STORE_VALIDATION_EVENT,
            event.eventId()
        );

        log.info(
            "Order status changed by store validation. eventId={}, orderId={}, status=PENDING->PAYMENT_PENDING",
            event.eventId(),
            event.orderId()
        );
    }

    private void handleRejected(Order order, StoreOrderValidationEventDto event) {
        if (order.getStatus() == Order.Status.FAILED || order.getStatus() == Order.Status.CANCELLED) {
            log.debug(
                "Order already rejected. eventId={}, orderId={}, currentStatus={}",
                event.eventId(),
                event.orderId(),
                order.getStatus()
            );
            return;
        }

        if (order.getStatus() != Order.Status.PENDING && order.getStatus() != Order.Status.PAYMENT_PENDING) {
            log.warn(
                "Store rejected event skipped due to unexpected status. eventId={}, orderId={}, currentStatus={}",
                event.eventId(),
                event.orderId(),
                order.getStatus()
            );
            return;
        }

        String reason = buildRejectedReason(event);
        Order.Status currentStatus = order.getStatus();
        order.updateStatus(Order.Status.FAILED, reason);
        Order savedOrder = orderRepository.save(order);
        recordOrderStatusHistoryService.record(
            savedOrder,
            currentStatus,
            Order.Status.FAILED,
            reason,
            OrderStatusHistory.SourceType.STORE_VALIDATION_EVENT,
            event.eventId()
        );

        log.info(
            "Order status changed by store rejection. eventId={}, orderId={}, status={} -> FAILED, rejectCode={}",
            event.eventId(),
            event.orderId(),
            event.validationResult(),
            event.rejectCode()
        );
    }

    private String buildRejectedReason(StoreOrderValidationEventDto event) {
        if (event.rejectCode() == null && event.rejectReason() == null) {
            return OrderStatusChangeReason.STORE_VALIDATION_REJECTED;
        }

        if (event.rejectCode() == null) {
            return event.rejectReason();
        }

        if (event.rejectReason() == null) {
            return event.rejectCode();
        }

        return "%s: %s".formatted(event.rejectCode(), event.rejectReason());
    }
}

