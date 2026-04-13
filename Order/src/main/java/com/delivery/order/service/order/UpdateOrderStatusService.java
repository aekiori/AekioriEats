package com.delivery.order.service.order;

import com.delivery.order.domain.order.Order;
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

    @Transactional
    public UpdateOrderStatusResultDto updateOrderStatus(Long orderId, UpdateOrderStatusDto updateOrderStatusDto) {
        Order order = orderReader.findOrder(orderId);
        return updateOrderStatus(order, updateOrderStatusDto);
    }

    @Transactional
    public UpdateOrderStatusResultDto updateOrderStatus(
        Long orderId,
        UpdateOrderStatusDto updateOrderStatusDto,
        String authenticatedUserRole
    ) {
        orderAuthorizationService.requireAdmin(authenticatedUserRole);
        Order order = orderReader.findOrder(orderId);
        return updateOrderStatus(order, updateOrderStatusDto);
    }

    @Transactional
    public UpdateOrderStatusResultDto cancelOrder(
        Long orderId,
        long authenticatedUserId,
        String authenticatedUserRole
    ) {
        Order order = orderReader.findOrder(orderId);
        orderAuthorizationService.requireSelfOrAdmin(
            authenticatedUserId,
            order.getUserId(),
            authenticatedUserRole
        );

        return updateOrderStatus(order, new UpdateOrderStatusDto(Order.Status.CANCELLED, null));
    }

    private UpdateOrderStatusResultDto updateOrderStatus(Order order, UpdateOrderStatusDto updateOrderStatusDto) {
        Long orderId = order.getId();
        Order.Status currentStatus = order.getStatus();
        Order.Status targetStatus = updateOrderStatusDto.status();

        log.info(
            "Order status update started. orderId={}, currentStatus={}, targetStatus={}",
            orderId, currentStatus, targetStatus
        );

        order.updateStatus(targetStatus, updateOrderStatusDto.reason());
        Order savedOrder = orderRepository.save(order);

        log.info(
            "Order status update completed. orderId={}, currentStatus={}, targetStatus={}",
            orderId, currentStatus, targetStatus
        );

        return UpdateOrderStatusResultDto.from(savedOrder);
    }
}
