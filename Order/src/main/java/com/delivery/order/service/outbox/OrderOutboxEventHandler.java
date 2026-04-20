package com.delivery.order.service.outbox;

import com.delivery.order.domain.order.event.OrderCreatedOutboxEvent;
import com.delivery.order.domain.order.event.OrderStatusChangedOutboxEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class OrderOutboxEventHandler {
    private final OrderOutboxService orderOutboxService;

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void handle(OrderCreatedOutboxEvent event) {
        orderOutboxService.saveOrderCreated(event);
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void handle(OrderStatusChangedOutboxEvent event) {
        orderOutboxService.saveOrderStatusChanged(event);
    }
}

