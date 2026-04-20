package com.delivery.order.service.outbox;

import com.delivery.order.constant.OrderEventType;
import com.delivery.order.domain.order.event.OrderCreatedOutboxEvent;
import com.delivery.order.domain.order.event.OrderStatusChangedOutboxEvent;
import com.delivery.order.domain.outbox.Outbox;
import com.delivery.order.dto.event.OrderCreatedEventDto;
import com.delivery.order.dto.event.OrderStatusChangedEventDto;
import com.delivery.order.dto.event.PaymentRequestedEventDto;
import com.delivery.order.exception.ApiException;
import com.delivery.order.repository.outbox.OutboxRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class OrderOutboxService {
    private static final Logger log = LoggerFactory.getLogger(OrderOutboxService.class);

    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    public void saveOrderCreated(OrderCreatedOutboxEvent event) {
        String eventId = getEventId();
        String payload = buildOrderCreatedPayload(event, eventId);

        Outbox outbox = new Outbox(
            eventId,
            Outbox.AggregateType.ORDER,
            event.orderId(),
            OrderEventType.ORDER_CREATED,
            payload,
            Outbox.Status.INIT,
            String.valueOf(event.userId())
        );

        outboxRepository.save(outbox);

        log.info("OrderCreated Outbox saved. orderId={}, eventId={}", event.orderId(), eventId);
    }

    public void saveOrderStatusChanged(OrderStatusChangedOutboxEvent event) {
        String eventId = getEventId();
        boolean paymentRequested = event.targetStatus() == com.delivery.order.domain.order.Order.Status.PAYMENT_PENDING;
        String eventType = paymentRequested
            ? OrderEventType.PAYMENT_REQUESTED
            : OrderEventType.ORDER_STATUS_CHANGED;
        String payload = paymentRequested
            ? buildPaymentRequestedPayload(event, eventId)
            : buildOrderStatusChangedPayload(event, eventId);

        Outbox outbox = new Outbox(
            eventId,
            Outbox.AggregateType.ORDER,
            event.orderId(),
            eventType,
            payload,
            Outbox.Status.INIT,
            String.valueOf(event.userId())
        );

        outboxRepository.save(outbox);

        log.info("OrderStatusChanged Outbox saved. orderId={}, eventId={}", event.orderId(), eventId);
    }

    private String buildOrderCreatedPayload(OrderCreatedOutboxEvent event, String eventId) {
        OrderCreatedEventDto payload = OrderCreatedEventDto.from(event, eventId, LocalDateTime.now());

        return serializePayload(payload, event.orderId());
    }

    private String buildOrderStatusChangedPayload(OrderStatusChangedOutboxEvent event, String eventId) {
        OrderStatusChangedEventDto payload = OrderStatusChangedEventDto.from(event, eventId, LocalDateTime.now());

        return serializePayload(payload, event.orderId());
    }

    private String buildPaymentRequestedPayload(OrderStatusChangedOutboxEvent event, String eventId) {
        PaymentRequestedEventDto payload = PaymentRequestedEventDto.from(event, eventId, LocalDateTime.now());

        return serializePayload(payload, event.orderId());
    }

    private String serializePayload(Object payload, Long orderId) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception exception) {
            log.error("Outbox payload serialization failed. orderId={}", orderId, exception);
            throw new ApiException(
                "OUTBOX_PAYLOAD_SERIALIZATION_ERROR",
                "Outbox payload 생성에 실패했다.",
                HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    private String getEventId() {
        return UUID.randomUUID().toString();
    }
}

