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
            if (result.accepted()) {
                history = StoreOrderValidation.createAccepted(event.orderId(), event.storeId(), validatedAt);
            } else {
                history = StoreOrderValidation.createRejected(
                    event.orderId(),
                    event.storeId(),
                    result.code(),
                    result.message(),
                    validatedAt
                );
            }

            return storeOrderValidationRepository.save(history);
        }

        if (result.accepted()) {
            history.markAccepted(validatedAt);
        } else {
            history.markRejected(result.code(), result.message(), validatedAt);
        }

        return history;
    }
}
