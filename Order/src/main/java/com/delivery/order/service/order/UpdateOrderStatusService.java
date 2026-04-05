package com.delivery.order.service.order;

import com.delivery.order.domain.order.Order;
import com.delivery.order.dto.request.UpdateOrderStatusDto;
import com.delivery.order.dto.response.UpdateOrderStatusResultDto;
import com.delivery.order.service.OrderOutboxService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UpdateOrderStatusService {
    private final OrderReader orderReader;
    private final OrderOutboxService orderOutboxService;

    @Transactional
    public UpdateOrderStatusResultDto updateOrderStatus(Long orderId, UpdateOrderStatusDto updateOrderStatusDto) {
        Order order = orderReader.findOrder(orderId);
        Order.Status currentStatus = order.getStatus();
        Order.Status targetStatus = updateOrderStatusDto.status();

        log.info(
            "Order status update started. orderId={}, currentStatus={}, targetStatus={}",
            orderId, currentStatus, targetStatus
        );

        order.updateStatus(targetStatus);

        String eventId = orderOutboxService.saveOrderStatusChanged(
            order, currentStatus, targetStatus, updateOrderStatusDto.reason()
        );

        log.info(
            "Order status update completed. orderId={}, eventId={}, currentStatus={}, targetStatus={}",
            orderId, eventId, currentStatus, targetStatus
        );

        return new UpdateOrderStatusResultDto(
            order.getId(),
            order.getStatus().name(),
            order.getUpdatedAt()
        );
    }
}
