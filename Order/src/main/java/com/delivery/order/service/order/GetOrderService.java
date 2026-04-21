package com.delivery.order.service.order;

import com.delivery.order.domain.order.Order;
import com.delivery.order.dto.response.OrderDetailResultDto;
import com.delivery.order.dto.response.OrderItemResultDto;
import com.delivery.order.dto.response.OrderStatusResultDto;
import com.delivery.order.repository.order.OrderItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GetOrderService {
    private final OrderReader orderReader;
    private final OrderItemRepository orderItemRepository;
    private final OrderAuthorizationService orderAuthorizationService;

    @Transactional(readOnly = true)
    public OrderDetailResultDto getOrder(Long orderId) {
        Order order = orderReader.findOrder(orderId);
        return buildOrderDetail(order);
    }

    @Transactional(readOnly = true)
    public OrderDetailResultDto getOrder(Long orderId, long authenticatedUserId) {
        Order order = orderReader.findOrder(orderId);
        orderAuthorizationService.requireSelf(authenticatedUserId, order.getUserId());
        return buildOrderDetail(order);
    }

    public OrderStatusResultDto getOrderStatus(Long orderId, long authenticatedUserId)
    {
        Order order = orderReader.findOrder(orderId);
        orderAuthorizationService.requireSelf(authenticatedUserId, order.getUserId());
        return OrderStatusResultDto.from(order);
    }

    private OrderDetailResultDto buildOrderDetail(Order order) {
        List<OrderItemResultDto> items = orderItemRepository.findByOrderId(order.getId()).stream()
            .map(OrderItemResultDto::from)
            .toList();

        return OrderDetailResultDto.from(order, items);
    }
}
