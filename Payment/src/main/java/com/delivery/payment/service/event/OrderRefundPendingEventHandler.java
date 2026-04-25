package com.delivery.payment.service.event;

import com.delivery.payment.domain.payment.Payment;
import com.delivery.payment.dto.event.OrderStatusChangedEventDto;
import com.delivery.payment.infra.portone.PortOnePaymentCancellation;
import com.delivery.payment.infra.portone.PortOnePaymentClient;
import com.delivery.payment.repository.outbox.OutboxRepository;
import com.delivery.payment.repository.payment.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderRefundPendingEventHandler {
    private final PaymentRepository paymentRepository;
    private final OutboxRepository outboxRepository;
    private final PortOnePaymentClient portOnePaymentClient;

    @Transactional
    public void handle(OrderStatusChangedEventDto event) {
        Payment payment = paymentRepository.findByOrderId(event.orderId()).orElse(null);
        if (payment == null) {
            log.warn(
                "Refund request ignored because payment was not found. eventId={}, orderId={}",
                event.eventId(),
                event.orderId()
            );
            return;
        }

        if (payment.getStatus() == Payment.Status.REFUNDED) {
            log.debug(
                "Payment already refunded. eventId={}, orderId={}, paymentId={}",
                event.eventId(),
                event.orderId(),
                payment.getId()
            );
            return;
        }

        if (payment.getStatus() != Payment.Status.SUCCESS) {
            log.warn(
                "Refund request skipped because payment status is not SUCCESS. eventId={}, orderId={}, paymentId={}, status={}",
                event.eventId(),
                event.orderId(),
                payment.getId(),
                payment.getStatus()
            );
            return;
        }

        String refundReason = buildRefundReason(event);
        PortOnePaymentCancellation cancellation = portOnePaymentClient.cancelPayment(
            payment.getPgTransactionId(),
            payment.getAmount(),
            refundReason
        );

        payment.refund(refundReason);
        publishPaymentRefundedEvent(payment, refundReason);

        log.info(
            "Payment refunded by store rejection. eventId={}, orderId={}, paymentId={}, providerPaymentId={}, cancellationId={}, amount={}",
            event.eventId(),
            event.orderId(),
            payment.getId(),
            cancellation.paymentId(),
            cancellation.cancellationId(),
            payment.getAmount()
        );
    }

    private String buildRefundReason(OrderStatusChangedEventDto event) {
        if (event.reason() == null || event.reason().isBlank()) {
            return "Store rejected order.";
        }

        return event.reason();
    }

    private void publishPaymentRefundedEvent(Payment payment, String refundReason) {
        outboxRepository.save(PaymentResultOutboxEvent.refunded(
            payment.getOrderId(),
            payment.getId(),
            payment.getAmount(),
            refundReason
        ));
    }
}
