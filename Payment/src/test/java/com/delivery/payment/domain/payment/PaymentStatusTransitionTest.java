package com.delivery.payment.domain.payment;

import com.delivery.payment.domain.payment.exception.InvalidPaymentStatusTransitionException;
import com.delivery.payment.dto.event.PaymentRequestedEventDto;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentStatusTransitionTest {
    @Test
    void payment_status_transitions_from_pending_to_success() {
        Payment payment = createRequestedPayment();

        payment.confirmSucceeded("pg-transaction-1");

        assertThat(payment.getStatus()).isEqualTo(Payment.Status.SUCCESS);
        assertThat(payment.getPgTransactionId()).isEqualTo("pg-transaction-1");
        assertThat(payment.getFailedReason()).isNull();
    }

    @Test
    void payment_status_transitions_from_pending_to_failed() {
        Payment payment = createRequestedPayment();

        payment.markFailed("PG_TIMEOUT");

        assertThat(payment.getStatus()).isEqualTo(Payment.Status.FAILED);
        assertThat(payment.getFailedReason()).isEqualTo("PG_TIMEOUT");
    }

    @Test
    void payment_status_transitions_from_success_to_refunded() {
        Payment payment = createRequestedPayment();
        payment.confirmSucceeded("pg-transaction-1");

        payment.refund("ORDER_CANCELED");

        assertThat(payment.getStatus()).isEqualTo(Payment.Status.REFUNDED);
        assertThat(payment.getFailedReason()).isEqualTo("ORDER_CANCELED");
    }

    @Test
    void payment_status_rejects_refund_when_not_success() {
        Payment payment = createRequestedPayment();

        assertThatThrownBy(() -> payment.refund("ORDER_CANCELED"))
            .isInstanceOf(InvalidPaymentStatusTransitionException.class)
            .hasMessageContaining("currentStatus=PENDING")
            .hasMessageContaining("targetStatus=REFUNDED");
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
