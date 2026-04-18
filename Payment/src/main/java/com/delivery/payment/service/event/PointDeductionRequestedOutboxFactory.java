package com.delivery.payment.service.event;

import com.delivery.payment.constant.PaymentEventType;
import com.delivery.payment.domain.outbox.Outbox;
import com.delivery.payment.dto.event.PointDeductionRequestedEventDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class PointDeductionRequestedOutboxFactory {
    private final ObjectMapper objectMapper;

    public Outbox create(Long orderId, Long paymentId, Long userId, Integer amount) {
        String eventId = UUID.randomUUID().toString();
        LocalDateTime occurredAt = LocalDateTime.now();
        PointDeductionRequestedEventDto payload = PointDeductionRequestedEventDto.from(
            eventId,
            PaymentEventType.POINT_DEDUCTION_REQUESTED,
            orderId,
            paymentId,
            userId,
            amount,
            occurredAt
        );

        return new Outbox(
            eventId,
            Outbox.AggregateType.POINT,
            userId,
            PaymentEventType.POINT_DEDUCTION_REQUESTED,
            serialize(payload),
            Outbox.Status.INIT,
            String.valueOf(orderId)
        );
    }

    private String serialize(PointDeductionRequestedEventDto payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception exception) {
            throw new IllegalStateException("Outbox payload serialization failed.", exception);
        }
    }
}
