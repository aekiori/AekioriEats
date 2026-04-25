package com.delivery.store.service.event;

import com.delivery.store.dto.event.OrderCreatedEventDto;
import com.delivery.store.repository.outbox.OutboxRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrderCreatedEventHandler {
    private final StoreOrderableValidator storeOrderableValidator;
    private final StoreOrderValidationHistoryService storeOrderValidationHistoryService;
    private final OutboxRepository outboxRepository;

    @Transactional
    public void handle(OrderCreatedEventDto event) {
        StoreOrderValidationResult result = storeOrderableValidator.validate(event);
        storeOrderValidationHistoryService.save(event, result);

        publishStoreOrderValidationResult(event, result);
    }

    private void publishStoreOrderValidationResult(
        OrderCreatedEventDto event,
        StoreOrderValidationResult result
    ) {
        if (result.accepted()) {
            outboxRepository.save(StoreOrderValidationOutboxEvent.accepted(event.orderId(), event.storeId()));
            return;
        }

        outboxRepository.save(
            StoreOrderValidationOutboxEvent.rejected(event.orderId(), event.storeId(), result.code(), result.message())
        );
    }
}
