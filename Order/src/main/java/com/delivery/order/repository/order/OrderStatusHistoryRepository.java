package com.delivery.order.repository.order;

import com.delivery.order.domain.order.OrderStatusHistory;
import com.delivery.order.dto.response.OrderStatusHistoryResultDto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OrderStatusHistoryRepository extends JpaRepository<OrderStatusHistory, Long> {
    @Query("""
        SELECT h.fromStatus AS fromStatus, h.toStatus AS toStatus, h.reason AS reason,
               h.sourceType AS sourceType, h.eventId AS eventId, h.createdAt AS createdAt
        FROM OrderStatusHistory h
        WHERE h.orderId = :orderId
        ORDER BY h.createdAt ASC
    """)
    List<OrderStatusHistoryResultDto> findStatusHistoryByOrderId(@Param("orderId") Long orderId);
}
