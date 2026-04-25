package com.delivery.order.service.order;

import com.delivery.order.domain.order.Order;
import com.delivery.order.domain.order.OrderStatusHistory;
import com.delivery.order.dto.request.UpdateOrderStatusRequestDto;
import com.delivery.order.dto.response.UpdateOrderStatusResponseDto;
import com.delivery.order.repository.order.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UpdateOrderStatusService {
    private final OrderReader orderReader;
    private final OrderRepository orderRepository;
    private final OrderAuthorizationService orderAuthorizationService;
    private final RecordOrderStatusHistoryService recordOrderStatusHistoryService;

    @Transactional
    public UpdateOrderStatusResponseDto updateOrderStatus(Long orderId, UpdateOrderStatusRequestDto updateOrderStatusDto) {
        Order order = orderReader.findOrder(orderId);
        return updateOrderStatus(order, updateOrderStatusDto);
    }

    @Transactional
    public UpdateOrderStatusResponseDto updateOrderStatus(
        Long orderId,
        UpdateOrderStatusRequestDto updateOrderStatusDto,
        long authenticatedUserId
    ) {
        Order order = orderReader.findOrder(orderId);
        orderAuthorizationService.requireSelf(authenticatedUserId, order.getUserId());
        return updateOrderStatus(order, updateOrderStatusDto);
    }

    @Transactional
    public UpdateOrderStatusResponseDto cancelOrder(
        Long orderId,
        long authenticatedUserId
    ) {
        Order order = orderReader.findOrder(orderId);
        orderAuthorizationService.requireSelf(authenticatedUserId, order.getUserId());

        return updateOrderStatus(order, new UpdateOrderStatusRequestDto(Order.Status.CANCELLED, null));
    }

    private UpdateOrderStatusResponseDto updateOrderStatus(Order order, UpdateOrderStatusRequestDto updateOrderStatusDto) {
        Long orderId = order.getId();
        Order.Status currentStatus = order.getStatus();
        Order.Status targetStatus = updateOrderStatusDto.status();
        String reason = updateOrderStatusDto.reason();

        log.info(
            "Order status update started. orderId={}, currentStatus={}, targetStatus={}",
            orderId, currentStatus, targetStatus
        );

        Order savedOrder = changeStatusByApi(order, currentStatus, targetStatus, reason);

        log.info(
            "Order status update completed. orderId={}, currentStatus={}, targetStatus={}",
            orderId, currentStatus, targetStatus
        );

        return UpdateOrderStatusResponseDto.from(savedOrder);
    }

    private Order changeStatusByApi(
        Order order,
        Order.Status currentStatus,
        Order.Status targetStatus,
        String reason
    ) {
        order.updateStatus(targetStatus, reason);
        Order savedOrder = orderRepository.save(order);
        recordOrderStatusHistoryService.record(
            savedOrder,
            currentStatus,
            targetStatus,
            reason,
            OrderStatusHistory.SourceType.API,
            null
        );
        return savedOrder;
    }
}
