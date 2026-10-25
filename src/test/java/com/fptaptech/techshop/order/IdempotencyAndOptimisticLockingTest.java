package com.fptaptech.techshop.order;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fptaptech.techshop.order.common.BadRequestException;
import com.fptaptech.techshop.order.common.IdempotencyConflictException;
import com.fptaptech.techshop.order.common.PreconditionFailedException;
import com.fptaptech.techshop.order.common.UserContext;
import com.fptaptech.techshop.order.order.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = OrderApplication.class)
@AutoConfigureMockMvc
class IdempotencyAndOptimisticLockingTest {

    @Autowired
    OrderService orderService;

    @Autowired
    ProductRepository productRepository;

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper objectMapper;

    private UserContext customerUser;
    private UserContext adminUser;
    private Long product1Id;
    private Long product2Id;

    @BeforeEach
    void setUp() {
        customerUser = new UserContext(1L, "customer@techshop.vn", UserContext.ROLE_CUSTOMER, "test-token");
        adminUser = new UserContext(999L, "admin@techshop.vn", UserContext.ROLE_ADMIN, "test-admin-token");

        List<Product> products = productRepository.findAll();
        product1Id = products.get(0).getId();
        product2Id = products.size() > 1 ? products.get(1).getId() : products.get(0).getId();
    }

    @Test
    @DisplayName("Idempotency Service: Cùng key + cùng payload trả về kết quả lưu trong cache")
    void testIdempotencyReturnsCachedResponseOnSamePayload() {
        String idempotencyKey = "idem-test-" + System.nanoTime();
        OrderRequest request = new OrderRequest(
                1L,
                "Địa chỉ kiểm tra Idempotency",
                List.of(new OrderItemRequest(product1Id, 2)),
                PaymentRequest.cod()
        );

        // First call -> creates order
        OrderResponseV2 firstResponse = orderService.createOrder(request, idempotencyKey, customerUser);
        assertNotNull(firstResponse);
        assertNotNull(firstResponse.code());

        // Second call with same key and same body -> returns identical cached response
        OrderResponseV2 secondResponse = orderService.createOrder(request, idempotencyKey, customerUser);
        assertEquals(firstResponse.code(), secondResponse.code());
        assertEquals(firstResponse.total().value(), secondResponse.total().value());
    }

    @Test
    @DisplayName("Idempotency Service: Cùng key + khác payload ném 409 IdempotencyConflictException")
    void testIdempotencyThrowsConflictOnDifferentPayload() {
        String idempotencyKey = "idem-conflict-" + System.nanoTime();
        OrderRequest request1 = new OrderRequest(
                1L,
                "Địa chỉ ban đầu",
                List.of(new OrderItemRequest(product1Id, 1)),
                PaymentRequest.cod()
        );

        orderService.createOrder(request1, idempotencyKey, customerUser);

        // Second call with same key but DIFFERENT body (different quantity or address)
        OrderRequest request2 = new OrderRequest(
                1L,
                "Địa chỉ bị thay đổi",
                List.of(new OrderItemRequest(product1Id, 5)),
                PaymentRequest.cod()
        );

        // Expect 409 IDEMPOTENCY_CONFLICT
        assertThrows(IdempotencyConflictException.class, () ->
                orderService.createOrder(request2, idempotencyKey, customerUser)
        );
    }

    @Test
    @DisplayName("Idempotency HTTP API: Thiếu header Idempotency-Key trả về 400 Bad Request RFC 9457")
    void testMissingIdempotencyKeyHeaderReturnsBadRequest() throws Exception {
        OrderRequest request = new OrderRequest(
                1L,
                "Địa chỉ không có Idempotency-Key",
                List.of(new OrderItemRequest(product1Id, 1)),
                PaymentRequest.cod()
        );

        mvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer dev-customer-token")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(header().string("Content-Type", org.hamcrest.Matchers.containsString("application/problem+json")))
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("Idempotency HTTP API: Gửi thành công trả về 201 Created và ETag, replay trả về kết quả")
    void testHttpIdempotentOrderCreationAndReplay() throws Exception {
        String key = "http-idem-" + System.nanoTime();
        OrderRequest request = new OrderRequest(
                1L,
                "Địa chỉ HTTP Idempotent Test",
                List.of(new OrderItemRequest(product1Id, 1)),
                PaymentRequest.cod()
        );

        // 1. First call: 201 Created
        mvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", key)
                        .header("Authorization", "Bearer dev-customer-token")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("ETag"))
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.code").isNotEmpty());

        // 2. Replay with identical key & body
        mvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", key)
                        .header("Authorization", "Bearer dev-customer-token")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("ETag"))
                .andExpect(jsonPath("$.code").isNotEmpty());
    }

    @Test
    @DisplayName("BR-02: Trùng productId trong cùng một đơn hàng ném BadRequestException")
    void testDuplicateProductInSameOrderThrowsBadRequest() {
        String idempotencyKey = "dup-prod-" + System.nanoTime();
        OrderRequest request = new OrderRequest(
                1L,
                "Địa chỉ kiểm tra trùng sản phẩm",
                List.of(
                        new OrderItemRequest(product1Id, 1),
                        new OrderItemRequest(product1Id, 2) // Trùng product1Id
                ),
                PaymentRequest.cod()
        );

        assertThrows(BadRequestException.class, () ->
                orderService.createOrder(request, idempotencyKey, customerUser)
        );
    }

    @Test
    @DisplayName("BR-06: Optimistic Locking - sai phiên bản ném 412 Precondition Failed, đúng phiên bản thành công")
    void testOptimisticLockingMismatchedVersionThrowsPreconditionFailed() {
        String key = "opt-lock-" + System.nanoTime();
        OrderRequest req = new OrderRequest(
                1L,
                "Địa chỉ ETag",
                List.of(new OrderItemRequest(product1Id, 1)),
                PaymentRequest.cod()
        );
        OrderResponseV2 order = orderService.createOrder(req, key, customerUser);

        // Passing wrong If-Match version (e.g. current is 0, passing "99")
        assertThrows(PreconditionFailedException.class, () ->
                orderService.transition(order.code(), new TransitionRequest(OrderAction.CONFIRM, "Test"), "99", adminUser)
        );

        // Passing null If-Match version
        assertThrows(PreconditionFailedException.class, () ->
                orderService.transition(order.code(), new TransitionRequest(OrderAction.CONFIRM, "Test"), null, adminUser)
        );

        // Passing correct If-Match version succeeds
        OrderResponseV2 updated = orderService.transition(order.code(), new TransitionRequest(OrderAction.CONFIRM, "OK"), String.valueOf(order.version()), adminUser);
        assertEquals(OrderStatus.CONFIRMED, updated.status());
    }
}

