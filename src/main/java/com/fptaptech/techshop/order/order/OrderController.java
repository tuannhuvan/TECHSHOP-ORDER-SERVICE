package com.fptaptech.techshop.order.order;

import com.fptaptech.techshop.order.common.SecurityContext;
import com.fptaptech.techshop.order.common.UserContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.math.BigDecimal;
import java.net.URI;
import java.time.Instant;
import java.util.List;

@RestController
public class OrderController implements OrderApi {

    private final OrderService orderService;
    private final InvoiceService invoiceService;
    private final OrderMapper orderMapper;

    public OrderController(OrderService orderService, InvoiceService invoiceService, OrderMapper orderMapper) {
        this.orderService = orderService;
        this.invoiceService = invoiceService;
        this.orderMapper = orderMapper;
    }

    @Override
    public ResponseEntity<OrderResponseV2> createOrder(String idempotencyKey, OrderRequest request) {
        UserContext user = SecurityContext.get().orElse(null);
        OrderResponseV2 response = orderService.createOrder(request, idempotencyKey, user);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{code}")
                .buildAndExpand(response.code())
                .toUri();

        return ResponseEntity.created(location)
                .eTag("\"" + response.version() + "\"")
                .body(response);
    }

    @Override
    public ResponseEntity<PageResponse<OrderResponseV2>> getOrders(
            OrderStatus status, Instant from, Instant to, BigDecimal minTotal, int page, int size, String sort) {

        UserContext user = SecurityContext.get().orElse(null);
        Long customerId = null;
        if (user != null && user.isCustomer()) {
            customerId = user.userId();
        }

        Sort sortOrder = Sort.by(Sort.Direction.DESC, "createdAt");
        if (sort != null && !sort.isBlank()) {
            String[] parts = sort.split(",");
            String prop = parts[0].trim();
            Sort.Direction dir = (parts.length > 1 && "asc".equalsIgnoreCase(parts[1].trim()))
                    ? Sort.Direction.ASC : Sort.Direction.DESC;
            if ("totalAmount".equalsIgnoreCase(prop) || "createdAt".equalsIgnoreCase(prop) || "status".equalsIgnoreCase(prop)) {
                sortOrder = Sort.by(dir, prop);
            }
        }

        Pageable pageable = PageRequest.of(page, size, sortOrder);
        Page<Order> orderPage = orderService.searchOrders(customerId, status, from, to, minTotal, pageable);
        Page<OrderResponseV2> dtoPage = orderPage.map(orderMapper::toResponseV2);

        return ResponseEntity.ok(PageResponse.of(dtoPage));
    }

    @Override
    public ResponseEntity<OrderResponseV2> getOrderByCode(String code) {
        Order order = orderService.getOrderEntityByCode(code);
        OrderResponseV2 response = orderMapper.toResponseV2(order);

        return ResponseEntity.ok()
                .eTag("\"" + response.version() + "\"")
                .body(response);
    }

    @Override
    public ResponseEntity<OrderResponseV1> getOrderByCodeV1(String code) {
        Order order = orderService.getOrderEntityByCode(code);
        OrderResponseV1 response = orderMapper.toResponseV1(order);

        return ResponseEntity.ok()
                .eTag("\"" + response.version() + "\"")
                .body(response);
    }

    @Override
    public ResponseEntity<OrderResponseV2> transitionOrder(String code, String ifMatch, TransitionRequest request) {
        UserContext actor = SecurityContext.get().orElse(null);
        OrderResponseV2 response = orderService.transition(code, request, ifMatch, actor);

        return ResponseEntity.ok()
                .eTag("\"" + response.version() + "\"")
                .body(response);
    }

    @Override
    public ResponseEntity<InvoiceResponse> uploadInvoice(String code, MultipartFile file) {
        InvoiceResponse response = invoiceService.uploadInvoice(code, file);
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<byte[]> getInvoicePdf(String code) {
        byte[] pdfBytes = invoiceService.getInvoicePdf(code);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"invoice-" + code + ".pdf\"")
                .body(pdfBytes);
    }

    @Override
    public ResponseEntity<List<OrderEventResponse>> getOrderEvents(String code) {
        List<OrderEventResponse> events = orderService.getOrderEvents(code);
        return ResponseEntity.ok(events);
    }
}
