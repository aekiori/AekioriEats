package com.delivery.order.service.order;

import com.delivery.order.domain.order.Order;
import com.delivery.order.exception.ApiException;
import com.delivery.order.repository.order.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrderReader {
    private final OrderRepository orderRepository;

    public Order findOrder(Long orderId) {
        return orderRepository.findById(orderId)
            .orElseThrow(() -> new ApiException(
                "ORDER_NOT_FOUND",
                "주문을 찾을 수 없다.",
                HttpStatus.NOT_FOUND
            ));
    }

    public Page<Order> getOrderPage(Long userId, Order.Status status, Pageable pageable) {
        if (status == null) {
            return orderRepository.findByUserId(userId, pageable);
        }

        return orderRepository.findByUserIdAndStatus(userId, status, pageable);
    }
}
