package com.delivery.point.service.event;

import com.delivery.point.constant.PointEventType;
import com.delivery.point.domain.outbox.Outbox;
import com.delivery.point.dto.event.PointDeductionResultEventDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.function.BiFunction;

@Component
@RequiredArgsConstructor
public class PointDeductionResultOutboxFactory {
    private final ObjectMapper objectMapper;

    public Outbox deducted(Long orderId, Long paymentId, Long userId, Integer amount) {
        return create(
            orderId,
            userId,
            PointEventType.POINT_DEDUCTED,
            (eventId, occurredAt) -> PointDeductionResultEventDto.deducted(
                eventId,
                PointEventType.POINT_DEDUCTED,
                orderId,
                paymentId,
                userId,
                amount,
                occurredAt
            )
        );
    }

    public Outbox failed(Long orderId, Long paymentId, Long userId, Integer amount, String reason) {
        return create(
            orderId,
            userId,
            PointEventType.POINT_DEDUCTION_FAILED,
            (eventId, occurredAt) -> PointDeductionResultEventDto.failed(
                eventId,
                PointEventType.POINT_DEDUCTION_FAILED,
                orderId,
                paymentId,
                userId,
                amount,
                reason,
                occurredAt
            )
        );
    }

    private Outbox create(
        Long orderId,
        Long userId,
        String eventType,
        BiFunction<String, LocalDateTime, PointDeductionResultEventDto> payloadFactory
    ) {
        String eventId = UUID.randomUUID().toString();
        LocalDateTime occurredAt = LocalDateTime.now();

        return new Outbox(
            eventId,
            Outbox.AggregateType.POINT,
            userId,
            eventType,
            serialize(payloadFactory.apply(eventId, occurredAt)),
            Outbox.Status.INIT,
            String.valueOf(orderId)
        );
    }

    private String serialize(PointDeductionResultEventDto payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception exception) {
            throw new IllegalStateException("Outbox payload serialization failed.", exception);
        }
    }
}
