package com.delivery.order.service;

import com.delivery.order.domain.order.event.OrderCreatedOutboxEvent;
import com.delivery.order.domain.order.event.OrderStatusChangedOutboxEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class OrderOutboxEventHandler { // note -- 이거 각 이벤트별 별도 핸들러로 가져갈지 고민이네 지금처럼 걍 다 때려넣어?
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
