package com.delivery.order.domain.order.exception;

import com.delivery.order.domain.order.Order;

public class InvalidOrderStatusTransitionException extends RuntimeException {
    public InvalidOrderStatusTransitionException(Order.Status currentStatus, Order.Status targetStatus) {
        super("허용되지 않는 주문 상태 변경. currentStatus=" + currentStatus + ", targetStatus=" + targetStatus);
    }
}
