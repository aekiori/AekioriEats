package com.delivery.payment.service.event;

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
    private final PaymentRepository paymentRepository;
    private final OutboxRepository outboxRepository;
    private final PointDeductionRequestedOutboxFactory pointDeductionRequestedOutboxFactory;

    @Transactional
    public void handle(PaymentRequestedEventDto event) {
        if (event.finalAmount() == null || event.finalAmount() < 0) {
            return;
        }

        if (paymentRepository.findByOrderId(event.orderId()).isPresent()) {
            return;
        }

        Payment payment = paymentRepository.save(Payment.requested(event));

        if (event.usedPointAmount() != null && event.usedPointAmount() > 0) {
            publishPointDeductionRequestedEvent(event, payment);
        }
    }

    private void publishPointDeductionRequestedEvent(
        PaymentRequestedEventDto event,
        Payment payment
    ) {
        outboxRepository.save(pointDeductionRequestedOutboxFactory.create(
            event.orderId(),
            payment.getId(),
            event.userId(),
            event.usedPointAmount()
        ));
    }
}
