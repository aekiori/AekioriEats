package com.delivery.store.service.event;

import com.delivery.store.dto.event.OrderStatusChangedEventDto;
import com.delivery.store.repository.store.StoreOrderRepository;
import com.delivery.store.domain.store.StoreOrder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderPaidEventHandler {
    private static final String PAID_STATUS = "PAID";

    private final StoreOrderRepository storeOrderRepository;

    @Transactional
    public void handle(OrderStatusChangedEventDto event) {
        if (!PAID_STATUS.equals(event.targetStatus())) {
            return;
        }

        if (storeOrderRepository.findByOrderId(event.orderId()).isPresent()) {
            log.debug("Store order already exists. eventId={}, orderId={}", event.eventId(), event.orderId());
            return;
        }

        StoreOrder storeOrder = StoreOrder.pending(
            event.orderId(),
            event.storeId(),
            event.userId(),
            event.finalAmount(),
            event.occurredAt()
        );
        storeOrderRepository.save(storeOrder);

        log.info("Store order pending created. eventId={}, orderId={}, storeId={}", event.eventId(), event.orderId(), event.storeId());
    }
}
