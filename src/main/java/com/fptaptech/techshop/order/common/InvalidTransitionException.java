package com.fptaptech.techshop.order.common;

import com.fptaptech.techshop.order.order.OrderAction;
import com.fptaptech.techshop.order.order.OrderStatus;

public class InvalidTransitionException extends RuntimeException {
    private final OrderStatus currentStatus;
    private final OrderAction action;

    public InvalidTransitionException(OrderStatus currentStatus, OrderAction action, String message) {
        super(message);
        this.currentStatus = currentStatus;
        this.action = action;
    }

    public OrderStatus getCurrentStatus() {
        return currentStatus;
    }

    public OrderAction getAction() {
        return action;
    }
}
