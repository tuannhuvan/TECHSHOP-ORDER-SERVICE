package com.fptaptech.techshop.order.order;

public enum OrderStatus {
    PENDING,
    CONFIRMED,
    SHIPPING,
    DELIVERED,
    CANCELLED,
    RETURN_REQUESTED,
    RETURNED;

    public boolean isTerminal() {
        return this == CANCELLED || this == RETURNED;
    }
}
