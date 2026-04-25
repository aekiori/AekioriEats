package com.delivery.order.service.order;

import com.delivery.order.domain.order.Order;
import com.delivery.order.domain.order.OrderItem;
import com.delivery.order.domain.order.OrderStatusHistory;
import com.delivery.order.constant.OrderStatusChangeReason;
import com.delivery.order.dto.request.CreateOrderRequestDto;
import com.delivery.order.dto.request.CreateOrderItemRequestDto;
import com.delivery.order.dto.response.CreateOrderResponseDto;
import com.delivery.order.exception.ApiException;
import com.delivery.order.repository.order.OrderItemRepository;
import com.delivery.order.repository.order.OrderRepository;
import com.delivery.order.service.idempotency.OrderIdempotencyCacheService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CreateOrderService {
    private static final Logger log = LoggerFactory.getLogger(CreateOrderService.class);

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderIdempotencyCacheService orderIdempotencyCacheService;
    private final OrderAuthorizationService orderAuthorizationService;
    private final RecordOrderStatusHistoryService recordOrderStatusHistoryService;
    private final ObjectMapper objectMapper;

    @Transactional
    public CreateOrderResponseDto createOrder(CreateOrderRequestDto request, String idempotencyKeyHeader) {
        String idempotencyKey = normalizeIdempotencyKey(idempotencyKeyHeader);
        String requestHash = generateRequestHash(request);

        CreateOrderResponseDto existing = resolveIdempotentResult(idempotencyKey, requestHash);
        if (existing != null) {
            return existing;
        }

        log.info("Order create started. userId={}, storeId={}", request.userId(), request.storeId());

        try {
            CreateOrderResponseDto result = processCreateOrder(request, idempotencyKey, requestHash);
            registerIdempotencyAfterCommit(idempotencyKey, result);
            return result;
        } catch (RuntimeException exception) {
            orderIdempotencyCacheService.release(idempotencyKey);
            throw exception;
        }
    }

    @Transactional
    public CreateOrderResponseDto createOrder(
        CreateOrderRequestDto request,
        String idempotencyKeyHeader,
        long authenticatedUserId
    ) {
        orderAuthorizationService.requireSelf(authenticatedUserId, request.userId());

        return createOrder(request, idempotencyKeyHeader);
    }

    private CreateOrderResponseDto resolveIdempotentResult(String idempotencyKey, String requestHash) {
        CreateOrderResponseDto cachedResult = orderIdempotencyCacheService.getCompletedResult(idempotencyKey);

        if (cachedResult != null) {
            log.info(
                "Idempotent order returned from Redis cache. idempotencyKey={}, orderId={}",
                idempotencyKey,
                cachedResult.orderId()
            );
            return cachedResult;
        }

        boolean acquired = orderIdempotencyCacheService.tryAcquire(idempotencyKey, requestHash);

        if (!acquired) {
            validateProcessingRequest(idempotencyKey, requestHash);
            throw new ApiException(
                "IDEMPOTENT_REQUEST_IN_PROGRESS",
                "Same idempotent request is already being processed.",
                HttpStatus.CONFLICT
            );
        }

        return null;
    }

    private CreateOrderResponseDto processCreateOrder(CreateOrderRequestDto request, String idempotencyKey, String requestHash) {
        Order existingOrder = orderRepository.findByIdempotencyKey(idempotencyKey).orElse(null);
        if (existingOrder != null) {
            return restoreExistingOrder(idempotencyKey, requestHash, existingOrder);
        }

        int totalAmount = calculateTotalAmount(request.items());
        int usedPointAmount = request.usedPointAmount();
        int finalAmount = totalAmount - usedPointAmount;

        validateFinalAmount(finalAmount);

        Order savedOrder;
        try {
            savedOrder = saveOrder(request, totalAmount, usedPointAmount, finalAmount, idempotencyKey, requestHash);
        } catch (DataIntegrityViolationException exception) {
            return resolveExistingOrderAfterDuplicateKey(idempotencyKey, requestHash, exception);
        }

        recordOrderCreatedHistory(savedOrder);
        List<OrderItem> orderItems = saveOrderItems(savedOrder, request.items());
        savedOrder.registerCreatedEvent(orderItems);
        orderRepository.save(savedOrder);

        log.info(
            "Order create completed. orderId={}, userId={}, finalAmount={}",
            savedOrder.getId(),
            savedOrder.getUserId(),
            savedOrder.getFinalAmount()
        );

        return CreateOrderResponseDto.from(savedOrder);
    }

    private CreateOrderResponseDto resolveExistingOrderAfterDuplicateKey(
        String idempotencyKey,
        String requestHash,
        DataIntegrityViolationException exception
    ) {
        Order existingOrder = orderRepository.findByIdempotencyKey(idempotencyKey)
            .orElseThrow(() -> exception);

        return restoreExistingOrder(idempotencyKey, requestHash, existingOrder);
    }

    private CreateOrderResponseDto restoreExistingOrder(String idempotencyKey, String requestHash, Order existingOrder) {
        validateSameRequest(existingOrder, requestHash);

        CreateOrderResponseDto result = CreateOrderResponseDto.from(existingOrder);
        orderIdempotencyCacheService.saveCompletedResult(idempotencyKey, result);

        log.info(
            "Existing idempotent order restored from DB. idempotencyKey={}, orderId={}",
            idempotencyKey,
            existingOrder.getId()
        );

        return result;
    }

    private void validateFinalAmount(int finalAmount) {
        if (finalAmount < 0) {
            throw new ApiException(
                "INVALID_AMOUNT",
                "Final amount must be zero or greater.",
                HttpStatus.BAD_REQUEST
            );
        }
    }

    private void validateProcessingRequest(String idempotencyKey, String requestHash) {
        String processingRequestHash = orderIdempotencyCacheService.getProcessingRequestHash(idempotencyKey);

        if (processingRequestHash != null && !processingRequestHash.equals(requestHash)) {
            throw new ApiException(
                "IDEMPOTENCY_KEY_CONFLICT",
                "Different request payload was submitted with the same idempotencyKey.",
                HttpStatus.CONFLICT
            );
        }
    }

    private void registerIdempotencyAfterCommit(String idempotencyKey, CreateOrderResponseDto result) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                orderIdempotencyCacheService.saveCompletedResult(idempotencyKey, result);
            }

            @Override
            public void afterCompletion(int status) {
                if (status != TransactionSynchronization.STATUS_COMMITTED) {
                    orderIdempotencyCacheService.release(idempotencyKey);
                }
            }
        });
    }

    private int calculateTotalAmount(List<CreateOrderItemRequestDto> items) {
        return items.stream()
            .mapToInt(item -> item.unitPrice() * item.quantity())
            .sum();
    }

    private Order saveOrder(
        CreateOrderRequestDto request,
        int totalAmount,
        int usedPointAmount,
        int finalAmount,
        String idempotencyKey,
        String requestHash
    ) {
        Order order = new Order(
            request.userId(),
            request.storeId(),
            Order.Status.PENDING,
            request.deliveryAddress(),
            totalAmount,
            usedPointAmount,
            finalAmount,
            idempotencyKey,
            requestHash
        );

        return orderRepository.save(order);
    }

    private void recordOrderCreatedHistory(Order savedOrder) {
        recordOrderStatusHistoryService.record(
            savedOrder,
            null,
            Order.Status.PENDING,
            OrderStatusChangeReason.ORDER_CREATED,
            OrderStatusHistory.SourceType.ORDER_CREATED,
            null
        );
    }

    private String normalizeIdempotencyKey(String idempotencyKey) {
        return idempotencyKey.trim();
    }

    private String generateRequestHash(CreateOrderRequestDto request) {
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("userId", request.userId());
            payload.put("storeId", request.storeId());
            payload.put("deliveryAddress", request.deliveryAddress());
            payload.put("usedPointAmount", request.usedPointAmount());
            payload.put("items",
                request.items().stream().map(item -> {
                    Map<String, Object> itemMap = new LinkedHashMap<>();
                    itemMap.put("menuId", item.menuId());
                    itemMap.put("menuName", item.menuName());
                    itemMap.put("unitPrice", item.unitPrice());
                    itemMap.put("quantity", item.quantity());
                    return itemMap;
                }).toList());

            String json = objectMapper.writeValueAsString(payload);
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");

            return HexFormat.of().formatHex(messageDigest.digest(json.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new ApiException(
                "REQUEST_HASH_GENERATION_ERROR",
                "Request hash generation failed.",
                HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    private void validateSameRequest(Order existingOrder, String requestHash) {
        if (existingOrder.getRequestHash() != null && !existingOrder.getRequestHash().equals(requestHash)) {
            throw new ApiException(
                "IDEMPOTENCY_KEY_CONFLICT",
                "Different request payload was submitted with the same idempotencyKey.",
                HttpStatus.CONFLICT
            );
        }
    }

    private List<OrderItem> saveOrderItems(Order order, List<CreateOrderItemRequestDto> items) {
        List<OrderItem> orderItems = items.stream()
            .map(item -> new OrderItem(
                order,
                item.menuId(),
                item.menuName(),
                item.unitPrice(),
                item.quantity(),
                item.unitPrice() * item.quantity()
            ))
            .toList();

        return orderItemRepository.saveAll(orderItems);
    }
}
