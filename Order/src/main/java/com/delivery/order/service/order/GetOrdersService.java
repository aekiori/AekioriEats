package com.delivery.order.service.order;

import com.delivery.order.domain.order.Order;
import com.delivery.order.dto.response.OrderPageResultDto;
import com.delivery.order.dto.response.OrderSummaryResultDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GetOrdersService {
    private final OrderReader orderReader;

    @Transactional(readOnly = true)
    public OrderPageResultDto getOrders(Long userId, Order.Status status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Order> orderPage = orderReader.getOrderPage(userId, status, pageable);

        List<OrderSummaryResultDto> content = orderPage.getContent().stream()
            .map(OrderSummaryResultDto::from)
            .toList();

        return OrderPageResultDto.from(orderPage, content);
    }
}
