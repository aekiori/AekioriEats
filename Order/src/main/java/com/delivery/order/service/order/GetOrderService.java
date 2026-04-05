package com.delivery.order.service.order;

import com.delivery.order.domain.order.Order;
import com.delivery.order.dto.response.OrderDetailResultDto;
import com.delivery.order.dto.response.OrderItemResultDto;
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

    @Transactional(readOnly = true)
    public OrderDetailResultDto getOrder(Long orderId) {
        Order order = orderReader.findOrder(orderId);
        List<OrderItemResultDto> items = orderItemRepository.findByOrderId(orderId).stream()
            .map(item -> new OrderItemResultDto(
                item.getMenuId(),
                item.getMenuName(),
                item.getUnitPrice(),
                item.getQuantity(),
                item.getLineAmount()
            ))
            .toList();

        return new OrderDetailResultDto(
            order.getId(),
            order.getUserId(),
            order.getStoreId(),
            order.getStatus().name(),
            order.getDeliveryAddress(),
            order.getTotalAmount(),
            order.getUsedPointAmount(),
            order.getFinalAmount(),
            items,
            order.getCreatedAt(),
            order.getUpdatedAt()
        );
    }
}
