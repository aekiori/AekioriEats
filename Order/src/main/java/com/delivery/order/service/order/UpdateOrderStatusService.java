package com.delivery.order.service.order;

import com.delivery.order.domain.order.Order;
import com.delivery.order.domain.order.OrderStatusHistory;
import com.delivery.order.dto.request.UpdateOrderStatusDto;
import com.delivery.order.dto.response.UpdateOrderStatusResultDto;
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
    public UpdateOrderStatusResultDto updateOrderStatus(Long orderId, UpdateOrderStatusDto updateOrderStatusDto) {
        Order order = orderReader.findOrder(orderId);
        return updateOrderStatus(order, updateOrderStatusDto);
    }

    @Transactional
    public UpdateOrderStatusResultDto updateOrderStatus(
        Long orderId,
        UpdateOrderStatusDto updateOrderStatusDto,
        long authenticatedUserId
    ) {
        Order order = orderReader.findOrder(orderId);
        orderAuthorizationService.requireSelf(authenticatedUserId, order.getUserId());
        return updateOrderStatus(order, updateOrderStatusDto);
    }

    @Transactional
    public UpdateOrderStatusResultDto cancelOrder(
        Long orderId,
        long authenticatedUserId
    ) {
        Order order = orderReader.findOrder(orderId);
        orderAuthorizationService.requireSelf(authenticatedUserId, order.getUserId());

        return updateOrderStatus(order, new UpdateOrderStatusDto(Order.Status.CANCELLED, null));
    }

    private UpdateOrderStatusResultDto updateOrderStatus(Order order, UpdateOrderStatusDto updateOrderStatusDto) {
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

        return UpdateOrderStatusResultDto.from(savedOrder);
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
