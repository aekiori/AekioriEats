package com.delivery.store.service.event;

import com.delivery.store.constant.OrderEventType;
import com.delivery.store.domain.outbox.Outbox;
import com.delivery.store.dto.event.StoreOrderDecisionEventDto;
import com.delivery.store.exception.ApiException;
import org.springframework.http.HttpStatus;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.UUID;

public final class StoreOrderDecisionOutboxEvent {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private StoreOrderDecisionOutboxEvent() {
    }

    public static Outbox accepted(Long orderId, Long storeId) {
        String eventId = UUID.randomUUID().toString();
        LocalDateTime occurredAt = LocalDateTime.now();
        StoreOrderDecisionEventDto payload = StoreOrderDecisionEventDto.accepted(
            eventId,
            OrderEventType.STORE_ORDER_ACCEPTED,
            orderId,
            storeId,
            occurredAt
        );
        return create(orderId, OrderEventType.STORE_ORDER_ACCEPTED, payload);
    }

    public static Outbox rejected(Long orderId, Long storeId, String rejectReason) {
        String eventId = UUID.randomUUID().toString();
        LocalDateTime occurredAt = LocalDateTime.now();
        StoreOrderDecisionEventDto payload = StoreOrderDecisionEventDto.rejected(
            eventId,
            OrderEventType.STORE_ORDER_REJECTED,
            orderId,
            storeId,
            rejectReason,
            occurredAt
        );
        return create(orderId, OrderEventType.STORE_ORDER_REJECTED, payload);
    }

    private static Outbox create(Long orderId, String eventType, StoreOrderDecisionEventDto payload) {
        return new Outbox(
            payload.eventId(),
            Outbox.AggregateType.ORDER,
            orderId,
            eventType,
            serialize(payload),
            Outbox.Status.INIT,
            String.valueOf(orderId)
        );
    }

    private static String serialize(StoreOrderDecisionEventDto payload) {
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
