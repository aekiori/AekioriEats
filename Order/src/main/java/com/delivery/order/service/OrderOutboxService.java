package com.delivery.order.service;

import com.delivery.order.constant.OrderEventType;
import com.delivery.order.domain.order.Order;
import com.delivery.order.domain.order.OrderItem;
import com.delivery.order.domain.outbox.Outbox;
import com.delivery.order.dto.event.OrderCreatedEventDto;
import com.delivery.order.dto.event.OrderStatusChangedEventDto;
import com.delivery.order.exception.ApiException;
import com.delivery.order.repository.outbox.OutboxRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class OrderOutboxService {
    private static final Logger log = LoggerFactory.getLogger(OrderOutboxService.class);

    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    public String saveOrderCreated(Order order, List<OrderItem> items) {
        String eventId = getEventId();
        String payload = buildOrderCreatedPayload(order, items, eventId);

        // todo -- DDD 관점에서 아박 생성을 이 서비스가 책임지는게 맞나 더 봐야할듯
        Outbox outbox = new Outbox(
            eventId,
            Outbox.AggregateType.ORDER,
            order.getId(),
            OrderEventType.ORDER_CREATED,
            payload,
            Outbox.Status.INIT,
            String.valueOf(order.getUserId())
        );

        outboxRepository.save(outbox);

        log.info("OrderCreated Outbox 저장 완료. orderId={}, eventId={}", order.getId(), eventId);
        return eventId;
    }

    public String saveOrderStatusChanged(
        Order order,
        Order.Status currentStatus,
        Order.Status targetStatus,
        String reason
    ) {
        String eventId = getEventId();
        String payload = buildOrderStatusChangedPayload(order, currentStatus, targetStatus, reason, eventId);

        // todo -- 얘도 위와같음
        Outbox outbox = new Outbox(
            eventId,
            Outbox.AggregateType.ORDER,
            order.getId(),
            OrderEventType.ORDER_STATUS_CHANGED,
            payload,
            Outbox.Status.INIT,
            String.valueOf(order.getUserId())
        );

        outboxRepository.save(outbox);

        log.info("OrderStatusChanged Outbox 저장 완료. orderId={}, eventId={}", order.getId(), eventId);

        return eventId;
    }

    private String buildOrderCreatedPayload(
        Order order,
        List<OrderItem> items,
        String eventId
    ) {
        OrderCreatedEventDto payload = new OrderCreatedEventDto(
            eventId,
            OrderEventType.ORDER_CREATED,
            LocalDateTime.now(),
            order.getId(),
            order.getUserId(),
            order.getStoreId(),
            order.getTotalAmount(),
            order.getUsedPointAmount(),
            order.getFinalAmount(),
            order.getStatus().name(),
            items.stream()
                .map(item -> new OrderCreatedEventDto.OrderCreatedItemDto(
                    item.getMenuId(),
                    item.getMenuName(),
                    item.getUnitPrice(),
                    item.getQuantity(),
                    item.getLineAmount()
                ))
                .toList()
        );

        return serializePayload(payload, order.getId());
    }

    private String buildOrderStatusChangedPayload(
        Order order,
        Order.Status currentStatus,
        Order.Status targetStatus,
        String reason,
        String eventId
    ) {
        OrderStatusChangedEventDto payload = new OrderStatusChangedEventDto(
            eventId,
            OrderEventType.ORDER_STATUS_CHANGED,
            LocalDateTime.now(),
            order.getId(),
            order.getUserId(),
            order.getStoreId(),
            currentStatus.name(),
            targetStatus.name(),
            reason
        );

        return serializePayload(payload, order.getId());
    }

    private String serializePayload(Object payload, Long orderId) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception exception) {
            log.error("Outbox payload 생성 실패. orderId={}", orderId, exception);
            throw new ApiException(
                "OUTBOX_PAYLOAD_SERIALIZATION_ERROR",
                "Outbox payload 생성에 실패했다.",
                HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    private String getEventId()
    {
        return UUID.randomUUID().toString();
    }
}
