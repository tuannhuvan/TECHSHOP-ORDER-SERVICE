package com.fptaptech.techshop.order;

import com.fptaptech.techshop.order.common.BadRequestException;
import com.fptaptech.techshop.order.common.ForbiddenException;
import com.fptaptech.techshop.order.common.InvalidTransitionException;
import com.fptaptech.techshop.order.common.UserContext;
import com.fptaptech.techshop.order.order.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = OrderApplication.class)
class OrderStateMachineTest {

    @Autowired
    OrderService orderService;

    @Autowired
    OrderRepository orderRepository;

    @Autowired
    ProductRepository productRepository;

    private UserContext adminUser;
    private UserContext customerUser;
    private UserContext partnerUser;
    private Long sampleProductId;

    @BeforeEach
    void setUp() {
        adminUser = new UserContext(999L, "admin@techshop.vn", UserContext.ROLE_ADMIN, "mock-admin-token");
        customerUser = new UserContext(1L, "customer@techshop.vn", UserContext.ROLE_CUSTOMER, "mock-cust-token");
        partnerUser = new UserContext(888L, "partner@carrier.vn", UserContext.ROLE_PARTNER, "mock-partner-key");

        Product p = productRepository.findAll().stream().findFirst().orElseThrow();
        sampleProductId = p.getId();
    }

    private OrderResponseV2 createTestPendingOrder() {
        String key = "test-order-" + System.nanoTime();
        OrderRequest req = new OrderRequest(
                1L,
                "Địa chỉ thử nghiệm máy trạng thái",
                List.of(new OrderItemRequest(sampleProductId, 1)),
                PaymentRequest.cod()
        );
        return orderService.createOrder(req, key, customerUser);
    }

    private OrderResponseV2 createOrderInState(OrderStatus targetStatus) {
        OrderResponseV2 order = createTestPendingOrder();
        if (targetStatus == OrderStatus.PENDING) return order;

        if (targetStatus == OrderStatus.CANCELLED) {
            return orderService.transition(order.code(), new TransitionRequest(OrderAction.CANCEL, "Khách hủy"), String.valueOf(order.version()), customerUser);
        }

        // CONFIRM
        order = orderService.transition(order.code(), new TransitionRequest(OrderAction.CONFIRM, "Admin duyệt"), String.valueOf(order.version()), adminUser);
        if (targetStatus == OrderStatus.CONFIRMED) return order;

        // SHIP
        order = orderService.transition(order.code(), new TransitionRequest(OrderAction.SHIP, "Giao bưu tá"), String.valueOf(order.version()), partnerUser);
        if (targetStatus == OrderStatus.SHIPPING) return order;

        // DELIVER
        order = orderService.transition(order.code(), new TransitionRequest(OrderAction.DELIVER, "Giao thành công"), String.valueOf(order.version()), partnerUser);
        if (targetStatus == OrderStatus.DELIVERED) return order;

        // REQUEST_RETURN
        order = orderService.transition(order.code(), new TransitionRequest(OrderAction.REQUEST_RETURN, "Lỗi sản phẩm"), String.valueOf(order.version()), customerUser);
        if (targetStatus == OrderStatus.RETURN_REQUESTED) return order;

        // ACCEPT_RETURN
        order = orderService.transition(order.code(), new TransitionRequest(OrderAction.ACCEPT_RETURN, "Admin nhận lại hàng"), String.valueOf(order.version()), adminUser);
        if (targetStatus == OrderStatus.RETURNED) return order;

        throw new IllegalArgumentException("Unknown status: " + targetStatus);
    }

    // ==========================================
    // 1. TẤT CẢ 8 CẶP CHUYỂN TRẠNG THÁI HỢP LỆ
    // ==========================================

    @Test
    @DisplayName("Hợp lệ 1: PENDING + CONFIRM -> CONFIRMED (Admin)")
    void testValidPendingToConfirmed() {
        OrderResponseV2 order = createTestPendingOrder();
        OrderResponseV2 updated = orderService.transition(order.code(), new TransitionRequest(OrderAction.CONFIRM, "Duyệt"), String.valueOf(order.version()), adminUser);
        assertEquals(OrderStatus.CONFIRMED, updated.status());
    }

    @Test
    @DisplayName("Hợp lệ 2: PENDING + CANCEL -> CANCELLED (Customer)")
    void testValidPendingToCancelledByCustomer() {
        OrderResponseV2 order = createTestPendingOrder();
        OrderResponseV2 updated = orderService.transition(order.code(), new TransitionRequest(OrderAction.CANCEL, "Đổi ý"), String.valueOf(order.version()), customerUser);
        assertEquals(OrderStatus.CANCELLED, updated.status());
    }

    @Test
    @DisplayName("Hợp lệ 3: CONFIRMED + SHIP -> SHIPPING (Admin/Partner)")
    void testValidConfirmedToShipping() {
        OrderResponseV2 order = createOrderInState(OrderStatus.CONFIRMED);
        OrderResponseV2 updated = orderService.transition(order.code(), new TransitionRequest(OrderAction.SHIP, "Xuất kho"), String.valueOf(order.version()), partnerUser);
        assertEquals(OrderStatus.SHIPPING, updated.status());
    }

    @Test
    @DisplayName("Hợp lệ 4: CONFIRMED + CANCEL -> CANCELLED (Admin có reason)")
    void testValidConfirmedToCancelledByAdminWithReason() {
        OrderResponseV2 order = createOrderInState(OrderStatus.CONFIRMED);
        OrderResponseV2 updated = orderService.transition(order.code(), new TransitionRequest(OrderAction.CANCEL, "Kho hết hàng đột xuất"), String.valueOf(order.version()), adminUser);
        assertEquals(OrderStatus.CANCELLED, updated.status());
    }

    @Test
    @DisplayName("Hợp lệ 5: SHIPPING + DELIVER -> DELIVERED (Partner, lưu deliveredAt)")
    void testValidShippingToDelivered() {
        OrderResponseV2 order = createOrderInState(OrderStatus.SHIPPING);
        OrderResponseV2 updated = orderService.transition(order.code(), new TransitionRequest(OrderAction.DELIVER, "Đã giao tận tay"), String.valueOf(order.version()), partnerUser);
        assertEquals(OrderStatus.DELIVERED, updated.status());
        assertNotNull(updated.deliveredAt());
    }

    @Test
    @DisplayName("Hợp lệ 6: SHIPPING + FAIL_DELIVERY -> CONFIRMED (Partner báo giao thất bại)")
    void testValidShippingToFailDeliveryReturnsToConfirmed() {
        OrderResponseV2 order = createOrderInState(OrderStatus.SHIPPING);
        OrderResponseV2 updated = orderService.transition(order.code(), new TransitionRequest(OrderAction.FAIL_DELIVERY, "Khách hẹn ngày mai"), String.valueOf(order.version()), partnerUser);
        assertEquals(OrderStatus.CONFIRMED, updated.status());
    }

    @Test
    @DisplayName("Hợp lệ 7: DELIVERED + REQUEST_RETURN -> RETURN_REQUESTED (Customer trong 7 ngày)")
    void testValidDeliveredToReturnRequested() {
        OrderResponseV2 order = createOrderInState(OrderStatus.DELIVERED);
        OrderResponseV2 updated = orderService.transition(order.code(), new TransitionRequest(OrderAction.REQUEST_RETURN, "Màn hình bị xước"), String.valueOf(order.version()), customerUser);
        assertEquals(OrderStatus.RETURN_REQUESTED, updated.status());
    }

    @Test
    @DisplayName("Hợp lệ 8: RETURN_REQUESTED + ACCEPT_RETURN -> RETURNED (Admin)")
    void testValidReturnRequestedToReturned() {
        OrderResponseV2 order = createOrderInState(OrderStatus.RETURN_REQUESTED);
        OrderResponseV2 updated = orderService.transition(order.code(), new TransitionRequest(OrderAction.ACCEPT_RETURN, "Đã kiểm hàng hoàn tiền"), String.valueOf(order.version()), adminUser);
        assertEquals(OrderStatus.RETURNED, updated.status());
    }

    // ==========================================
    // 2. TẤT CẢ 41 CẶP CHUYỂN TRẠNG THÁI KHÔNG HỢP LỆ
    // ==========================================

    @Test
    @DisplayName("Kiểm tra toàn bộ 41 cặp chuyển trạng thái KHÔNG HỢP LỆ trên 7 trạng thái")
    void testAllInvalidTransitionPairs() {
        // Ma trận các hành động hợp lệ cho từng trạng thái:
        Map<OrderStatus, Set<OrderAction>> validActionsMap = Map.of(
                OrderStatus.PENDING, Set.of(OrderAction.CONFIRM, OrderAction.CANCEL),
                OrderStatus.CONFIRMED, Set.of(OrderAction.SHIP, OrderAction.CANCEL),
                OrderStatus.SHIPPING, Set.of(OrderAction.DELIVER, OrderAction.FAIL_DELIVERY),
                OrderStatus.DELIVERED, Set.of(OrderAction.REQUEST_RETURN),
                OrderStatus.RETURN_REQUESTED, Set.of(OrderAction.ACCEPT_RETURN),
                OrderStatus.RETURNED, Set.of(),   // Trạng thái kết thúc - cấm mọi action
                OrderStatus.CANCELLED, Set.of()   // Trạng thái kết thúc - cấm mọi action
        );

        int invalidTransitionsChecked = 0;

        for (OrderStatus status : OrderStatus.values()) {
            Set<OrderAction> validActions = validActionsMap.get(status);
            Set<OrderAction> invalidActions = EnumSet.allOf(OrderAction.class);
            invalidActions.removeAll(validActions);

            for (OrderAction invalidAction : invalidActions) {
                // Tạo đơn hàng ở trạng thái tương ứng
                OrderResponseV2 order = createOrderInState(status);

                // Chạy thử hành động không hợp lệ -> Bắt buộc ném InvalidTransitionException (409 Conflict)
                final OrderResponseV2 currentOrder = order;
                assertThrows(InvalidTransitionException.class, () -> {
                    orderService.transition(
                            currentOrder.code(),
                            new TransitionRequest(invalidAction, "Thử nghiệm chuyển không hợp lệ"),
                            String.valueOf(currentOrder.version()),
                            adminUser
                    );
                }, "Kỳ vọng ném InvalidTransitionException cho cặp: " + status + " -> " + invalidAction);

                invalidTransitionsChecked++;
            }
        }

        // Kiểm tra đúng 41 cặp không hợp lệ đã được test
        assertEquals(41, invalidTransitionsChecked, "Tổng số cặp chuyển trạng thái không hợp lệ phải chính xác là 41!");
    }

    // ==========================================
    // 3. KIỂM THỬ CÁC RÀNG BUỘC NGHIỆP VỤ BIÊN (EDGE CASES)
    // ==========================================

    @Test
    @DisplayName("BR-04: Hủy đơn CONFIRMED bắt buộc phải có reason (ADMIN)")
    void testCancelConfirmedWithoutReasonThrowsBadRequest() {
        OrderResponseV2 order = createOrderInState(OrderStatus.CONFIRMED);
        assertThrows(BadRequestException.class, () ->
                orderService.transition(order.code(), new TransitionRequest(OrderAction.CANCEL, ""), String.valueOf(order.version()), adminUser)
        );
    }

    @Test
    @DisplayName("BR-04: Khách hàng không thể hủy đơn CONFIRMED (Forbidden)")
    void testCustomerCannotCancelConfirmedOrder() {
        OrderResponseV2 order = createOrderInState(OrderStatus.CONFIRMED);
        assertThrows(ForbiddenException.class, () ->
                orderService.transition(order.code(), new TransitionRequest(OrderAction.CANCEL, "Khách muốn hủy"), String.valueOf(order.version()), customerUser)
        );
    }

    @Test
    @DisplayName("BR-05: Yêu cầu trả hàng quá 7 ngày kể từ khi DELIVERED bị từ chối")
    void testReturnPolicyExceeded7DaysThrowsBadRequest() {
        OrderResponseV2 order = createOrderInState(OrderStatus.DELIVERED);

        // Giả lập deliveredAt là 8 ngày trước
        Order entity = orderRepository.findByCode(order.code()).orElseThrow();
        entity.setDeliveredAt(Instant.now().minus(8, ChronoUnit.DAYS));
        Order saved = orderRepository.saveAndFlush(entity);

        // Khách yêu cầu trả hàng -> Phải ném BadRequestException (BR-05)
        assertThrows(BadRequestException.class, () ->
                orderService.transition(order.code(), new TransitionRequest(OrderAction.REQUEST_RETURN, "Muốn trả hàng"), String.valueOf(saved.getVersion()), customerUser)
        );
    }

    @Test
    @DisplayName("Phân quyền: Khách hàng không có quyền duyệt đơn CONFIRM")
    void testCustomerCannotConfirmPendingOrder() {
        OrderResponseV2 order = createTestPendingOrder();
        assertThrows(ForbiddenException.class, () ->
                orderService.transition(order.code(), new TransitionRequest(OrderAction.CONFIRM, "Khách tự duyệt"), String.valueOf(order.version()), customerUser)
        );
    }
}

