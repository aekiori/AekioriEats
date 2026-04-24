package com.delivery.order.service.order;

import com.delivery.order.domain.order.Order;
import com.delivery.order.domain.order.OrderStatusHistory;
import com.delivery.order.repository.order.OrderStatusHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RecordOrderStatusHistoryService {
    private final OrderStatusHistoryRepository orderStatusHistoryRepository;

    public void record(
        Order order,
        Order.Status fromStatus,
        Order.Status toStatus,
        String reason,
        OrderStatusHistory.SourceType sourceType,
        String eventId
    ) {
        orderStatusHistoryRepository.save(
            new OrderStatusHistory(order.getId(), fromStatus, toStatus, reason, sourceType, eventId)
        );
    }
}
