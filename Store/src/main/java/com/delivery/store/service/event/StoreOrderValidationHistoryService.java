package com.delivery.store.service.event;

import com.delivery.store.domain.store.StoreOrderValidation;
import com.delivery.store.dto.event.OrderCreatedEventDto;
import com.delivery.store.repository.store.StoreOrderValidationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class StoreOrderValidationHistoryService {
    private final StoreOrderValidationRepository storeOrderValidationRepository;

    @Transactional
    public StoreOrderValidation save(OrderCreatedEventDto event, StoreOrderValidationResult result) {
        StoreOrderValidation history = storeOrderValidationRepository
            .findTopByOrderIdOrderByIdDesc(event.orderId())
            .orElse(null);

        LocalDateTime validatedAt = LocalDateTime.now();

        if (history == null) {
            return storeOrderValidationRepository.save(createHistory(event, result, validatedAt));
        }

        updateHistory(history, result, validatedAt);

        return history;
    }

    private StoreOrderValidation createHistory(
        OrderCreatedEventDto event,
        StoreOrderValidationResult result,
        LocalDateTime validatedAt
    ) {
        if (result.accepted()) {
            return StoreOrderValidation.createAccepted(event.orderId(), event.storeId(), validatedAt);
        }

        return StoreOrderValidation.createRejected(
            event.orderId(),
            event.storeId(),
            result.code(),
            result.message(),
            validatedAt
        );
    }

    private void updateHistory(
        StoreOrderValidation history,
        StoreOrderValidationResult result,
        LocalDateTime validatedAt
    ) {
        if (result.accepted()) {
            history.markAccepted(validatedAt);
            return;
        }

        history.markRejected(result.code(), result.message(), validatedAt);
    }
}
