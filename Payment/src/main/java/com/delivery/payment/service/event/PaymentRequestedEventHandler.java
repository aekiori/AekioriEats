package com.delivery.payment.service.event;

import com.delivery.payment.domain.payment.Payment;
import com.delivery.payment.dto.event.PaymentRequestedEventDto;
import com.delivery.payment.repository.payment.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PaymentRequestedEventHandler {
    private final PaymentRepository paymentRepository;

    @Transactional
    public void handle(PaymentRequestedEventDto event) {
        if (event.finalAmount() == null || event.finalAmount() < 0) {
            return;
        }

        Payment payment = paymentRepository.findByOrderId(event.orderId())
            .orElseGet(() -> paymentRepository.save(Payment.requested(event)));

        if (payment.getStatus() == Payment.Status.PENDING) {
            return;
        }
    }
}
