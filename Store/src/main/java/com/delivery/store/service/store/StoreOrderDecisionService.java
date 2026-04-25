package com.delivery.store.service.store;

import com.delivery.store.domain.outbox.Outbox;
import com.delivery.store.domain.store.Store;
import com.delivery.store.domain.store.StoreOrder;
import com.delivery.store.dto.request.owner.DecideStoreOrderRequestDto;
import com.delivery.store.dto.request.owner.GetStoreOrdersRequestDto;
import com.delivery.store.dto.response.StoreOrderDecisionResponseDto;
import com.delivery.store.dto.response.StoreOrderResponseDto;
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
    public List<StoreOrderResponseDto> getStoreOrders(
        Long storeId,
        GetStoreOrdersRequestDto request,
        long authenticatedUserId
    ) {
        Store store = storeRepository.findById(storeId)
            .orElseThrow(() -> new ApiException("STORE_NOT_FOUND", "Store not found.", HttpStatus.NOT_FOUND));
        storeAuthorizationService.requireStoreOwner(authenticatedUserId, store.getOwnerUserId());

        return storeOrderRepository.findByStoreIdAndStatusOrderByCreatedAtDesc(storeId, request.resolvedStatus())
            .stream()
            .map(StoreOrderResponseDto::from)
            .toList();
    }

    @Transactional
    public StoreOrderDecisionResponseDto decide(
        Long storeId,
        Long orderId,
        DecideStoreOrderRequestDto request,
        long authenticatedUserId
    ) {
        Store store = storeRepository.findById(storeId)
            .orElseThrow(() -> new ApiException("STORE_NOT_FOUND", "Store not found.", HttpStatus.NOT_FOUND));
        storeAuthorizationService.requireStoreOwner(authenticatedUserId, store.getOwnerUserId());

        StoreOrder storeOrder = storeOrderRepository.findByOrderId(orderId)
            .orElseThrow(() -> new ApiException("STORE_ORDER_NOT_FOUND", "Store order not found.", HttpStatus.NOT_FOUND));

        if (!storeOrder.getStoreId().equals(storeId)) {
            throw new ApiException("STORE_ORDER_MISMATCH", "Store order does not belong to this store.", HttpStatus.BAD_REQUEST);
        }

        StoreOrder.Status targetStatus = toStatus(request.decision());
        if (storeOrder.getStatus() == targetStatus) {
            return StoreOrderDecisionResponseDto.from(storeOrder);
        }

        if (storeOrder.getStatus() != StoreOrder.Status.PENDING) {
            throw new ApiException(
                "STORE_ORDER_ALREADY_DECIDED",
                "Store order decision is already completed.",
                HttpStatus.CONFLICT
            );
        }

        LocalDateTime decidedAt = LocalDateTime.now();
        Outbox outbox = decideStoreOrder(storeOrder, request, storeId, orderId, decidedAt);

        storeOrderRepository.save(storeOrder);
        outboxRepository.save(outbox);
        return StoreOrderDecisionResponseDto.from(storeOrder);
    }

    private Outbox decideStoreOrder(
        StoreOrder storeOrder,
        DecideStoreOrderRequestDto request,
        Long storeId,
        Long orderId,
        LocalDateTime decidedAt
    ) {
        if (request.decision() == DecideStoreOrderRequestDto.Decision.ACCEPTED) {
            storeOrder.accept(decidedAt);
            return StoreOrderDecisionOutboxEvent.accepted(orderId, storeId);
        }

        String rejectReason = normalizeRejectReason(request.rejectReason());
        storeOrder.reject(rejectReason, decidedAt);
        return StoreOrderDecisionOutboxEvent.rejected(orderId, storeId, rejectReason);
    }

    private String normalizeRejectReason(String rejectReason) {
        if (rejectReason == null || rejectReason.isBlank()) {
            return "Store rejected the order.";
        }
        return rejectReason.trim();
    }

    private StoreOrder.Status toStatus(DecideStoreOrderRequestDto.Decision decision) {
        if (decision == DecideStoreOrderRequestDto.Decision.ACCEPTED) {
            return StoreOrder.Status.ACCEPTED;
        }
        return StoreOrder.Status.REJECTED;
    }
}
