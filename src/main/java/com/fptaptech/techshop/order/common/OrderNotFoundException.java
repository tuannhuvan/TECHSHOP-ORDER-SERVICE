package com.fptaptech.techshop.order.common;

public class OrderNotFoundException extends RuntimeException {
    private final String orderCode;

    public OrderNotFoundException(String orderCode) {
        super("Không tìm thấy đơn hàng mã: " + orderCode);
        this.orderCode = orderCode;
    }

    public String getOrderCode() {
        return orderCode;
    }
}
