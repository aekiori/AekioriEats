package com.delivery.store.service.event;

import com.delivery.store.constant.OrderEventType;
import com.delivery.store.domain.outbox.Outbox;
import com.delivery.store.dto.event.StoreOrderValidationEventDto;
import com.delivery.store.exception.ApiException;
import org.springframework.http.HttpStatus;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.function.BiFunction;

public final class StoreOrderValidationOutboxEvent {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private StoreOrderValidationOutboxEvent() {
    }

    public static Outbox accepted(Long orderId, Long storeId) {
        return create(
            orderId,
            storeId,
            OrderEventType.ORDER_VALIDATED,
            (eventId, occurredAt) -> StoreOrderValidationEventDto.accepted(
                eventId,
                OrderEventType.ORDER_VALIDATED,
                orderId,
                storeId,
                occurredAt
            )
        );
    }

    public static Outbox rejected(Long orderId, Long storeId, String rejectCode, String rejectReason) {
        return create(
            orderId,
            storeId,
            OrderEventType.ORDER_REJECTED,
            (eventId, occurredAt) -> StoreOrderValidationEventDto.rejected(
                eventId,
                OrderEventType.ORDER_REJECTED,
                orderId,
                storeId,
                rejectCode,
                rejectReason,
                occurredAt
            )
        );
    }

    private static Outbox create(
        Long orderId,
        Long storeId,
        String eventType,
        BiFunction<String, LocalDateTime, StoreOrderValidationEventDto> payloadFactory
    ) {
        String eventId = UUID.randomUUID().toString();
        LocalDateTime occurredAt = LocalDateTime.now();

        return new Outbox(
            eventId,
            Outbox.AggregateType.ORDER,
            orderId,
            eventType,
            serialize(payloadFactory.apply(eventId, occurredAt)),
            Outbox.Status.INIT,
            String.valueOf(orderId)
        );
    }

    private static String serialize(StoreOrderValidationEventDto payload) {
        try {
            return OBJECT_MAPPER.writeValueAsString(payload);
        } catch (Exception exception) {
            throw new ApiException(
                "OUTBOX_PAYLOAD_SERIALIZATION_ERROR",
                "Outbox payload serialization failed.",
                HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }
}
