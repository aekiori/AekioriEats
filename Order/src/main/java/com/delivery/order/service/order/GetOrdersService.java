package com.delivery.order.service.order;

import com.delivery.order.domain.order.Order;
import com.delivery.order.dto.request.GetOrdersRequestDto;
import com.delivery.order.dto.response.OrderPageResponseDto;
import com.delivery.order.dto.response.OrderSummaryResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GetOrdersService {
    private final OrderReader orderReader;

    @Transactional(readOnly = true)
    public OrderPageResponseDto getOrders(Long userId, GetOrdersRequestDto request) {
        Pageable pageable = PageRequest.of(
            request.resolvedPage(),
            request.resolvedLimit(),
            Sort.by(Sort.Direction.DESC, "createdAt")
        );
        Page<Order> orderPage = orderReader.getOrderPage(userId, request.status(), pageable);

        List<OrderSummaryResponseDto> content = orderPage.getContent().stream()
            .map(OrderSummaryResponseDto::from)
            .toList();

        return OrderPageResponseDto.from(orderPage, content);
    }
}
