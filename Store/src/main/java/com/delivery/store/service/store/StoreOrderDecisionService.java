package com.delivery.store.service.store;

import com.delivery.store.domain.outbox.Outbox;
import com.delivery.store.domain.store.Store;
import com.delivery.store.domain.store.StoreOrder;
import com.delivery.store.dto.request.owner.DecideStoreOrderRequest;
import com.delivery.store.dto.response.StoreOrderDecisionResultDto;
import com.delivery.store.dto.response.StoreOrderResultDto;
import com.delivery.store.exception.ApiException;
import com.delivery.store.repository.outbox.OutboxRepository;
import com.delivery.store.repository.store.StoreOrderRepository;
import com.delivery.store.repository.store.StoreRepository;
import com.delivery.store.service.event.StoreOrderDecisionOutboxEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StoreOrderDecisionService {
    private final StoreRepository storeRepository;
    private final StoreOrderRepository storeOrderRepository;
    private final StoreAuthorizationService storeAuthorizationService;
    private final OutboxRepository outboxRepository;

    @Transactional(readOnly = true)
    public List<StoreOrderResultDto> getStoreOrders(
        Long storeId,
        StoreOrder.Status status,
        long authenticatedUserId,
        String authenticatedUserRole
    ) {
        Store store = storeRepository.findById(storeId)
            .orElseThrow(() -> new ApiException("STORE_NOT_FOUND", "Store not found.", HttpStatus.NOT_FOUND));
        storeAuthorizationService.requireStoreOwnerOrAdmin(
            authenticatedUserId,
            store.getOwnerUserId(),
            authenticatedUserRole
        );

        return storeOrderRepository.findByStoreIdAndStatusOrderByCreatedAtDesc(storeId, status)
            .stream()
            .map(StoreOrderResultDto::from)
            .toList();
    }

    @Transactional
    public StoreOrderDecisionResultDto decide(
        Long storeId,
        Long orderId,
        DecideStoreOrderRequest request,
        long authenticatedUserId,
        String authenticatedUserRole
    ) {
        Store store = storeRepository.findById(storeId)
            .orElseThrow(() -> new ApiException("STORE_NOT_FOUND", "Store not found.", HttpStatus.NOT_FOUND));
        storeAuthorizationService.requireStoreOwnerOrAdmin(
            authenticatedUserId,
            store.getOwnerUserId(),
            authenticatedUserRole
        );

        StoreOrder storeOrder = storeOrderRepository.findByOrderId(orderId)
            .orElseThrow(() -> new ApiException("STORE_ORDER_NOT_FOUND", "Store order not found.", HttpStatus.NOT_FOUND));

        if (!storeOrder.getStoreId().equals(storeId)) {
            throw new ApiException("STORE_ORDER_MISMATCH", "Store order does not belong to this store.", HttpStatus.BAD_REQUEST);
        }

        StoreOrder.Status targetStatus = toStatus(request.decision());
        if (storeOrder.getStatus() == targetStatus) {
            return StoreOrderDecisionResultDto.from(storeOrder);
        }

        if (storeOrder.getStatus() != StoreOrder.Status.PENDING) {
            throw new ApiException(
                "STORE_ORDER_ALREADY_DECIDED",
                "Store order decision is already completed.",
                HttpStatus.CONFLICT
            );
        }

        LocalDateTime decidedAt = LocalDateTime.now();
        Outbox outbox;
        if (request.decision() == DecideStoreOrderRequest.Decision.ACCEPTED) {
            storeOrder.accept(decidedAt);
            outbox = StoreOrderDecisionOutboxEvent.accepted(orderId, storeId);
        } else {
            String rejectReason = normalizeRejectReason(request.rejectReason());
            storeOrder.reject(rejectReason, decidedAt);
            outbox = StoreOrderDecisionOutboxEvent.rejected(orderId, storeId, rejectReason);
        }

        storeOrderRepository.save(storeOrder);
        outboxRepository.save(outbox);
        return StoreOrderDecisionResultDto.from(storeOrder);
    }

    private String normalizeRejectReason(String rejectReason) {
        if (rejectReason == null || rejectReason.isBlank()) {
            return "Store rejected the order.";
        }
        return rejectReason.trim();
    }

    private StoreOrder.Status toStatus(DecideStoreOrderRequest.Decision decision) {
        if (decision == DecideStoreOrderRequest.Decision.ACCEPTED) {
            return StoreOrder.Status.ACCEPTED;
        }
        return StoreOrder.Status.REJECTED;
    }
}
