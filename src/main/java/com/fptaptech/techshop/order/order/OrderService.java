package com.fptaptech.techshop.order.order;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fptaptech.techshop.order.common.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderEventRepository orderEventRepository;
    private final IdempotencyKeyRepository idempotencyKeyRepository;
    private final ProductRepository productRepository;
    private final OrderMapper orderMapper;
    private final ObjectMapper objectMapper;

    public OrderService(
            OrderRepository orderRepository,
            OrderItemRepository orderItemRepository,
            OrderEventRepository orderEventRepository,
            IdempotencyKeyRepository idempotencyKeyRepository,
            ProductRepository productRepository,
            OrderMapper orderMapper,
            @org.springframework.beans.factory.annotation.Autowired(required = false) ObjectMapper objectMapper) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.orderEventRepository = orderEventRepository;
        this.idempotencyKeyRepository = idempotencyKeyRepository;
        this.productRepository = productRepository;
        this.orderMapper = orderMapper;
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper().findAndRegisterModules();
    }

    public OrderResponseV2 createOrder(OrderRequest request, String idempotencyKey, UserContext userContext) {
        // BR-03: Header Idempotency-Key bắt buộc
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new BadRequestException("Header Idempotency-Key là bắt buộc khi tạo đơn hàng (BR-03)");
        }

        String requestHash = computeSha256(request);

        // Check existing idempotency key
        Optional<IdempotencyKeyRecord> existingKeyOpt = idempotencyKeyRepository.findById(idempotencyKey);
        if (existingKeyOpt.isPresent()) {
            IdempotencyKeyRecord existing = existingKeyOpt.get();
            // Check 24 hours validity
            if (existing.getCreatedAt() != null && existing.getCreatedAt().plus(24, ChronoUnit.HOURS).isAfter(Instant.now())) {
                if (existing.getRequestHash().equals(requestHash)) {
                    // Return cached response
                    try {
                        return objectMapper.readValue(existing.getResponseBody(), OrderResponseV2.class);
                    } catch (Exception e) {
                        // Fallback to reload
                    }
                } else {
                    // Same key, different body -> 409 IDEMPOTENCY_CONFLICT
                    throw new IdempotencyConflictException(idempotencyKey,
                            "Header Idempotency-Key đã được sử dụng với nội dung đơn hàng khác (BR-03)");
                }
            }
        }

        // BR-02: Kiểm tra số lượng và không trùng lặp productId
        if (request.items() == null || request.items().isEmpty()) {
            throw new BadRequestException("Đơn hàng phải chứa ít nhất 1 sản phẩm (BR-02)");
        }
        if (request.items().size() > 50) {
            throw new BadRequestException("Đơn hàng không được chứa quá 50 dòng sản phẩm (BR-02)");
        }

        long distinctProducts = request.items().stream()
                .map(OrderItemRequest::productId)
                .distinct()
                .count();
        if (distinctProducts < request.items().size()) {
            throw new BadRequestException("Đơn hàng không được chứa các dòng sản phẩm trùng lặp productId (BR-02)");
        }

        // BR-01: Server-Side Price Calculation
        Order order = new Order();
        order.setCode(generateOrderCode());
        order.setCustomerId(request.customerId());
        order.setShippingAddress(request.shippingAddress());
        order.setStatus(OrderStatus.PENDING);
        order.setCurrency("VND");

        // Payment setup
        Payment payment;
        if (request.payment().method() == PaymentMethod.CARD) {
            payment = Payment.card(request.payment().cardProvider(), request.payment().cardLast4());
        } else if (request.payment().method() == PaymentMethod.BANK_TRANSFER) {
            payment = Payment.bankTransfer(request.payment().bankCode(), request.payment().transferRef());
        } else {
            payment = Payment.cod();
        }
        order.setPayment(payment);

        for (OrderItemRequest itemReq : request.items()) {
            if (itemReq.quantity() < 1 || itemReq.quantity() > 20) {
                throw new BadRequestException("Số lượng cho sản phẩm ID=" + itemReq.productId() + " phải từ 1 đến 20 (BR-02)");
            }

            Product product = productRepository.findById(itemReq.productId())
                    .orElseThrow(() -> new BadRequestException("Không tìm thấy sản phẩm có ID=" + itemReq.productId()));

            // Snapshot product name and unit price
            OrderItem item = new OrderItem(product.getId(), product.getName(), product.getPrice(), itemReq.quantity());
            order.addItem(item);
        }

        order.recalculateTotalAmount();
        Order savedOrder = orderRepository.saveAndFlush(order);

        // Audit Trail
        String actor = userContext != null ? userContext.email() : "CUSTOMER";
        OrderEvent createEvent = new OrderEvent(savedOrder, OrderStatus.PENDING, OrderStatus.PENDING, OrderAction.CONFIRM, actor, "Tạo đơn hàng mới");
        orderEventRepository.save(createEvent);

        OrderResponseV2 response = orderMapper.toResponseV2(savedOrder);

        // Save idempotency record
        try {
            String responseJson = objectMapper.writeValueAsString(response);
            idempotencyKeyRepository.save(new IdempotencyKeyRecord(idempotencyKey, requestHash, responseJson));
        } catch (Exception ignored) {}

        return response;
    }

    public Order getOrderEntityByCode(String code) {
        return orderRepository.findByCode(code)
                .orElseThrow(() -> new OrderNotFoundException(code));
    }

    public OrderResponseV2 transition(String code, TransitionRequest request, String ifMatchHeader, UserContext actor) {
        Order order = getOrderEntityByCode(code);

        // BR-06: Optimistic Locking & ETag (If-Match check)
        if (ifMatchHeader == null || ifMatchHeader.isBlank()) {
            throw new PreconditionFailedException(null, String.valueOf(order.getVersion()),
                    "Header If-Match là bắt buộc khi thực hiện chuyển trạng thái đơn hàng (BR-06)");
        }

        String cleanedIfMatch = ifMatchHeader.replace("\"", "").trim();
        if (!cleanedIfMatch.equals(String.valueOf(order.getVersion()))) {
            throw new PreconditionFailedException(cleanedIfMatch, String.valueOf(order.getVersion()),
                    "Phiên bản đơn hàng đã thay đổi (Expected: " + order.getVersion() + ", Got: " + cleanedIfMatch + ") (BR-06)");
        }

        OrderStatus currentStatus = order.getStatus();
        OrderAction action = request.action();
        OrderStatus nextStatus;
        String actorRole = actor != null ? actor.role() : "ANONYMOUS";
        String actorEmail = actor != null ? actor.email() : "SYSTEM";

        // Validate state transition matrix
        switch (currentStatus) {
            case PENDING -> {
                if (action == OrderAction.CONFIRM) {
                    if (actor != null && !actor.isAdmin()) {
                        throw new ForbiddenException("Chỉ ADMIN mới có quyền duyệt đơn hàng (CONFIRM)");
                    }
                    nextStatus = OrderStatus.CONFIRMED;
                } else if (action == OrderAction.CANCEL) {
                    // Both CUSTOMER and ADMIN can cancel PENDING
                    nextStatus = OrderStatus.CANCELLED;
                } else {
                    throw new InvalidTransitionException(currentStatus, action,
                            "Từ PENDING chỉ có thể thực hiện CONFIRM hoặc CANCEL");
                }
            }
            case CONFIRMED -> {
                if (action == OrderAction.SHIP) {
                    if (actor != null && !actor.isAdmin() && !actor.isPartner()) {
                        throw new ForbiddenException("Chỉ ADMIN hoặc PARTNER mới có quyền giao vận chuyển (SHIP)");
                    }
                    nextStatus = OrderStatus.SHIPPING;
                } else if (action == OrderAction.CANCEL) {
                    // BR-04: Only ADMIN can cancel CONFIRMED order and MUST have reason
                    if (actor != null && !actor.isAdmin()) {
                        throw new ForbiddenException("Khách hàng không thể hủy khi đơn đã CONFIRMED. Chỉ ADMIN mới có quyền hủy đơn (BR-04)");
                    }
                    if (request.reason() == null || request.reason().isBlank()) {
                        throw new BadRequestException("Hủy đơn hàng ở trạng thái CONFIRMED bắt buộc phải điền lý do (reason) (BR-04)");
                    }
                    nextStatus = OrderStatus.CANCELLED;
                } else {
                    throw new InvalidTransitionException(currentStatus, action,
                            "Từ CONFIRMED chỉ có thể thực hiện SHIP hoặc CANCEL");
                }
            }
            case SHIPPING -> {
                if (action == OrderAction.DELIVER) {
                    if (actor != null && !actor.isPartner() && !actor.isAdmin()) {
                        throw new ForbiddenException("Chỉ PARTNER giao vận hoặc ADMIN mới có quyền xác nhận giao thành công (DELIVER)");
                    }
                    nextStatus = OrderStatus.DELIVERED;
                    order.setDeliveredAt(Instant.now());
                } else if (action == OrderAction.FAIL_DELIVERY) {
                    if (actor != null && !actor.isPartner() && !actor.isAdmin()) {
                        throw new ForbiddenException("Chỉ PARTNER giao vận mới có quyền báo giao thất bại (FAIL_DELIVERY)");
                    }
                    nextStatus = OrderStatus.CONFIRMED; // Quay về CONFIRMED để giao lại
                } else {
                    throw new InvalidTransitionException(currentStatus, action,
                            "Từ SHIPPING chỉ có thể thực hiện DELIVER hoặc FAIL_DELIVERY");
                }
            }
            case DELIVERED -> {
                if (action == OrderAction.REQUEST_RETURN) {
                    // BR-05: Return policy within 7 days
                    if (order.getDeliveredAt() != null) {
                        Instant deadline = order.getDeliveredAt().plus(7, ChronoUnit.DAYS);
                        if (Instant.now().isAfter(deadline)) {
                            throw new BadRequestException("Yêu cầu trả hàng chỉ hợp lệ trong vòng 7 ngày kể từ khi giao thành công (BR-05)");
                        }
                    }
                    nextStatus = OrderStatus.RETURN_REQUESTED;
                } else {
                    throw new InvalidTransitionException(currentStatus, action,
                            "Từ DELIVERED chỉ có thể thực hiện REQUEST_RETURN");
                }
            }
            case RETURN_REQUESTED -> {
                if (action == OrderAction.ACCEPT_RETURN) {
                    if (actor != null && !actor.isAdmin()) {
                        throw new ForbiddenException("Chỉ ADMIN mới có quyền chấp nhận trả hàng (ACCEPT_RETURN)");
                    }
                    nextStatus = OrderStatus.RETURNED;
                } else {
                    throw new InvalidTransitionException(currentStatus, action,
                            "Từ RETURN_REQUESTED chỉ có thể thực hiện ACCEPT_RETURN");
                }
            }
            default -> throw new InvalidTransitionException(currentStatus, action,
                    "Đơn hàng đang ở trạng thái kết thúc (" + currentStatus + "), không thể thực hiện thêm hành động nào");
        }

        order.setStatus(nextStatus);
        Order savedOrder = orderRepository.saveAndFlush(order);

        // Record Audit Event
        OrderEvent event = new OrderEvent(savedOrder, currentStatus, nextStatus, action, actorEmail, request.reason());
        orderEventRepository.save(event);

        return orderMapper.toResponseV2(savedOrder);
    }

    public Page<Order> searchOrders(Long customerId, OrderStatus status, Instant from, Instant to, BigDecimal minTotal, Pageable pageable) {
        Specification<Order> spec = (root, query, cb) -> cb.conjunction();

        if (customerId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("customerId"), customerId));
        }
        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }
        if (from != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("createdAt"), from));
        }
        if (to != null) {
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("createdAt"), to));
        }
        if (minTotal != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("totalAmount"), minTotal));
        }

        return orderRepository.findAll(spec, pageable);
    }

    public List<OrderEventResponse> getOrderEvents(String orderCode) {
        Order order = getOrderEntityByCode(orderCode);
        List<OrderEvent> events = orderEventRepository.findByOrderIdOrderByCreatedAtDesc(order.getId());
        return orderMapper.toEventResponses(events);
    }

    private String generateOrderCode() {
        return "ORD-2026-" + String.format("%06d", new Random().nextInt(1_000_000));
    }

    private String computeSha256(Object object) {
        try {
            String json = objectMapper.writeValueAsString(object);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(json.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException | com.fasterxml.jackson.core.JsonProcessingException e) {
            return UUID.randomUUID().toString();
        }
    }
}
