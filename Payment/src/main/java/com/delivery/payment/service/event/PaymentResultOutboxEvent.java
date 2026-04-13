package com.delivery.payment.service.event;

import com.delivery.payment.constant.PaymentEventType;
import com.delivery.payment.domain.outbox.Outbox;
import com.delivery.payment.dto.event.PaymentResultEventDto;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.function.BiFunction;

public final class PaymentResultOutboxEvent {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private PaymentResultOutboxEvent() {
    }

    public static Outbox succeeded(Long orderId, Long paymentId, Integer finalAmount, Integer usedPointAmount) {
        return create(
            orderId,
            paymentId,
            PaymentEventType.PAYMENT_SUCCEEDED,
            (eventId, occurredAt) -> PaymentResultEventDto.succeeded(
                eventId,
                PaymentEventType.PAYMENT_SUCCEEDED,
                orderId,
                paymentId,
                finalAmount,
                usedPointAmount,
                occurredAt
            )
        );
    }

    public static Outbox failed(Long orderId, Long paymentId, String failReason) {
        return create(
            orderId,
            paymentId,
            PaymentEventType.PAYMENT_FAILED,
            (eventId, occurredAt) -> PaymentResultEventDto.failed(
                eventId,
                PaymentEventType.PAYMENT_FAILED,
                orderId,
                paymentId,
                failReason,
                occurredAt
            )
        );
    }

    private static Outbox create(
        Long orderId,
        Long paymentId,
        String eventType,
        BiFunction<String, LocalDateTime, PaymentResultEventDto> payloadFactory
    ) {
        String eventId = UUID.randomUUID().toString();
        LocalDateTime occurredAt = LocalDateTime.now();

        return new Outbox(
            eventId,
            Outbox.AggregateType.PAYMENT,
            paymentId,
            eventType,
            serialize(payloadFactory.apply(eventId, occurredAt)),
            Outbox.Status.INIT,
            String.valueOf(orderId)
        );
    }

    private static String serialize(PaymentResultEventDto payload) {
        try {
            return OBJECT_MAPPER.writeValueAsString(payload);
        } catch (Exception exception) {
            throw new IllegalStateException("Outbox payload serialization failed.", exception);
        }
    }
}
