package com.delivery.payment.service.payment;

import com.delivery.payment.domain.payment.Payment;
import com.delivery.payment.dto.request.ConfirmPaymentRequest;
import com.delivery.payment.dto.response.ConfirmPaymentResponse;
import com.delivery.payment.infra.portone.PortOnePaymentClient;
import com.delivery.payment.infra.portone.PortOnePaymentVerification;
import com.delivery.payment.repository.outbox.OutboxRepository;
import com.delivery.payment.repository.payment.PaymentRepository;
import com.delivery.payment.service.event.PaymentResultOutboxEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConfirmPaymentService {
    private final PaymentRepository paymentRepository;
    private final OutboxRepository outboxRepository;
    private final PortOnePaymentClient portOnePaymentClient;
    private final PaymentAuthorizationService paymentAuthorizationService;

    @Transactional
    public ConfirmPaymentResponse confirm(ConfirmPaymentRequest request, long authenticatedUserId) {
        validateRequest(request);

        Payment payment = paymentRepository.findByOrderId(request.orderId())
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Payment request is not ready for orderId=" + request.orderId()
            ));

        paymentAuthorizationService.requireSelf(authenticatedUserId, payment.getUserId());

        log.info("payment confirm {}", payment);

        if (!request.amount().equals(payment.getAmount())) {
            log.info("콘푸로스트");
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Payment amount does not match order amount."
            );
        }

        boolean providerVerified = false;
        if (portOnePaymentClient.isVerifyEnabled()) {
            PortOnePaymentVerification verification = portOnePaymentClient.getPayment(request.paymentId());
            validateProviderPayment(verification, request);
            providerVerified = true;
        }

        if (payment.getStatus() == Payment.Status.SUCCESS) {
            return toResponse(payment, request.paymentId(), providerVerified);
        }

        if (payment.getStatus().isTerminal()) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Payment cannot be confirmed from status=" + payment.getStatus()
            );
        }

        payment.confirmSucceeded(request.paymentId());
        publishPaymentSucceededEvent(payment);

        return toResponse(payment, request.paymentId(), providerVerified);
    }

    private void publishPaymentSucceededEvent(Payment payment) {
        outboxRepository.save(
            PaymentResultOutboxEvent.succeeded(
                payment.getOrderId(),
                payment.getId(),
                payment.getAmount(),
                0
            )
        );
    }

    private void validateRequest(ConfirmPaymentRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request body is required.");
        }
        if (request.orderId() == null || request.orderId() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "orderId must be positive.");
        }
        if (request.paymentId() == null || request.paymentId().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "paymentId is required.");
        }
        if (request.amount() == null || request.amount() < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "amount must be zero or positive.");
        }
    }

    private void validateProviderPayment(
        PortOnePaymentVerification verification,
        ConfirmPaymentRequest request
    ) {
        if (verification == null) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "PortOne payment response is empty.");
        }
        if (!verification.isPaid()) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "PortOne payment is not paid. status=" + verification.status()
            );
        }
        if (!request.amount().equals(verification.amount())) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "PortOne payment amount does not match request amount."
            );
        }
    }

    private ConfirmPaymentResponse toResponse(
        Payment payment,
        String fallbackProviderPaymentId,
        boolean providerVerified
    ) {
        String providerPaymentId = payment.getPgTransactionId() != null
            ? payment.getPgTransactionId()
            : fallbackProviderPaymentId;

        return new ConfirmPaymentResponse(
            payment.getOrderId(),
            payment.getId(),
            providerPaymentId,
            payment.getStatus().name(),
            payment.getAmount(),
            providerVerified
        );
    }
}
