package com.delivery.payment.service.event;

import com.delivery.payment.domain.outbox.Outbox;
import com.delivery.payment.domain.payment.Payment;
import com.delivery.payment.dto.event.PaymentRequestedEventDto;
import com.delivery.payment.repository.outbox.OutboxRepository;
import com.delivery.payment.repository.payment.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PaymentRequestedEventHandler {
    private static final String INVALID_FINAL_AMOUNT = "INVALID_FINAL_AMOUNT";

    private final PaymentRepository paymentRepository;
    private final OutboxRepository outboxRepository;

    @Transactional
    public void handle(PaymentRequestedEventDto event) {
        Payment payment = paymentRepository.findByOrderId(event.orderId())
            .orElseGet(() -> paymentRepository.save(Payment.requested(event)));

        if (payment.getStatus() == Payment.Status.SUCCEEDED || payment.getStatus() == Payment.Status.FAILED) {
            return;
        }

        if (event.finalAmount() == null || event.finalAmount() < 0) {
            payment.markFailed(INVALID_FINAL_AMOUNT);
            outboxRepository.save(
                PaymentResultOutboxEvent.failed(
                    event.orderId(),
                    payment.getId(),
                    INVALID_FINAL_AMOUNT
                )
            );
            return;
        }

        payment.markSucceeded();
        outboxRepository.save(
            PaymentResultOutboxEvent.succeeded(
                event.orderId(),
                payment.getId(),
                event.finalAmount(),
                event.usedPointAmount()
            )
        );
    }
}
