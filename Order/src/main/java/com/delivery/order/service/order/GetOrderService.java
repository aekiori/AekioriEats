package com.delivery.order.service.order;

import com.delivery.order.domain.order.Order;
import com.delivery.order.dto.response.OrderDetailResponseDto;
import com.delivery.order.dto.response.OrderItemResponseDto;
import com.delivery.order.dto.response.OrderStatusHistoryResponseDto;
import com.delivery.order.dto.response.OrderStatusResponseDto;
import com.delivery.order.repository.order.OrderItemRepository;
import com.delivery.order.repository.order.OrderStatusHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GetOrderService {
    private final OrderReader orderReader;
    private final OrderItemRepository orderItemRepository;
    private final OrderStatusHistoryRepository orderStatusHistoryRepository;
    private final OrderAuthorizationService orderAuthorizationService;

    @Transactional(readOnly = true)
    public OrderDetailResponseDto getOrder(Long orderId) {
        Order order = orderReader.findOrder(orderId);
        return buildOrderDetail(order);
    }

    @Transactional(readOnly = true)
    public OrderDetailResponseDto getOrder(Long orderId, long authenticatedUserId) {
        Order order = orderReader.findOrder(orderId);
        orderAuthorizationService.requireSelf(authenticatedUserId, order.getUserId());
        return buildOrderDetail(order);
    }

    public OrderStatusResponseDto getOrderStatus(Long orderId, long authenticatedUserId)
    {
        Order order = orderReader.findOrder(orderId);
        orderAuthorizationService.requireSelf(authenticatedUserId, order.getUserId());
        return OrderStatusResponseDto.from(order);
    }

    private OrderDetailResponseDto buildOrderDetail(Order order) {
        List<OrderItemResponseDto> items = orderItemRepository.findByOrderId(order.getId()).stream()
            .map(OrderItemResponseDto::from)
            .toList();
        List<OrderStatusHistoryResponseDto> statusHistories =
            orderStatusHistoryRepository.findStatusHistoryByOrderId(order.getId());

        return OrderDetailResponseDto.from(order, items, statusHistories);
    }
}
