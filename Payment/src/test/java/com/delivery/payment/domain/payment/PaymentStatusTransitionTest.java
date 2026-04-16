package com.delivery.payment.domain.payment;

import com.delivery.payment.domain.payment.exception.InvalidPaymentStatusTransitionException;
import com.delivery.payment.dto.event.PaymentRequestedEventDto;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentStatusTransitionTest {
    @Test
    void payment_status_transitions_to_succeeded_through_processing() {
        Payment payment = createRequestedPayment();

        payment.markProcessing();
        payment.markSucceeded();

        assertThat(payment.getStatus()).isEqualTo(Payment.Status.SUCCEEDED);
        assertThat(payment.getProcessedAt()).isNotNull();
    }

    @Test
    void payment_status_rejects_direct_init_to_succeeded_transition() {
        Payment payment = createRequestedPayment();

        assertThatThrownBy(payment::markSucceeded)
            .isInstanceOf(InvalidPaymentStatusTransitionException.class)
            .hasMessageContaining("currentStatus=INIT")
            .hasMessageContaining("targetStatus=SUCCEEDED");
    }

    @Test
    void payment_status_transitions_to_canceled_through_cancel_requested() {
        Payment payment = createRequestedPayment();
        payment.markProcessing();
        payment.markSucceeded();

        payment.requestCancel();
        payment.markCanceled();

        assertThat(payment.getStatus()).isEqualTo(Payment.Status.CANCELED);
    }

    @Test
    void payment_status_rejects_cancel_request_when_not_succeeded() {
        Payment payment = createRequestedPayment();
        payment.markProcessing();
        payment.markFailed("PG_TIMEOUT");

        assertThatThrownBy(payment::requestCancel)
            .isInstanceOf(InvalidPaymentStatusTransitionException.class)
            .hasMessageContaining("currentStatus=FAILED")
            .hasMessageContaining("targetStatus=CANCEL_REQUESTED");
    }

    private Payment createRequestedPayment() {
        return Payment.requested(
            new PaymentRequestedEventDto(
                "event-1",
                "PaymentRequested",
                1,
                LocalDateTime.now(),
                1L,
                10L,
                20L,
                15000,
                1000,
                14000,
                "PAYMENT_PENDING"
            )
        );
    }
}
