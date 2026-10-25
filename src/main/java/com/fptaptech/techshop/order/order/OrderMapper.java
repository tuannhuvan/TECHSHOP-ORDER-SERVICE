package com.fptaptech.techshop.order.order;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.Named;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface OrderMapper {

    @Mapping(target = "payment", source = "payment")
    @Mapping(target = "items", source = "items")
    OrderResponseV1 toResponseV1(Order order);

    List<OrderResponseV1> toResponsesV1(List<Order> orders);

    @Mapping(target = "total", source = "order", qualifiedByName = "mapTotalMoney")
    @Mapping(target = "payment", source = "payment")
    @Mapping(target = "items", source = "items")
    OrderResponseV2 toResponseV2(Order order);

    List<OrderResponseV2> toResponsesV2(List<Order> orders);

    OrderItemResponse toItemResponse(OrderItem item);

    List<OrderItemResponse> toItemResponses(List<OrderItem> items);

    PaymentResponse toPaymentResponse(Payment payment);

    OrderEventResponse toEventResponse(OrderEvent event);

    List<OrderEventResponse> toEventResponses(List<OrderEvent> events);

    @Named("mapTotalMoney")
    default MoneyResponse mapTotalMoney(Order order) {
        if (order == null || order.getTotalAmount() == null) {
            return null;
        }
        return new MoneyResponse(order.getTotalAmount(), order.getCurrency() != null ? order.getCurrency() : "VND");
    }
}
