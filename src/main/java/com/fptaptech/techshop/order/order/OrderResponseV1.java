package com.fptaptech.techshop.order.order;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Schema(description = "Thông tin chi tiết đơn hàng (Public Spec V1 - Deprecated)")
public record OrderResponseV1(
        @Schema(description = "ID nội bộ", example = "1")
        Long id,

        @Schema(description = "Mã đơn hàng", example = "ORD-2026-000123")
        String code,

        @Schema(description = "ID khách hàng", example = "1")
        Long customerId,

        @Schema(description = "Trạng thái đơn hàng", example = "CONFIRMED")
        OrderStatus status,

        @Schema(description = "Địa chỉ nhận hàng", example = "Toà nhà FPT, Cầu Giấy, Hà Nội")
        String shippingAddress,

        @Deprecated
        @Schema(
                description = "Tổng tiền đơn hàng dạng số (VND). Thuộc tính này đã bị Deprecated, vui lòng chuyển qua sử dụng spec v2 với đối tượng tiền tệ đa quốc gia.",
                example = "1590000.00",
                deprecated = true
        )
        BigDecimal totalAmount,

        @Schema(description = "Thông tin thanh toán")
        PaymentResponse payment,

        @Schema(description = "Danh sách sản phẩm trong đơn")
        List<OrderItemResponse> items,

        @Schema(description = "Phiên bản đơn hàng (phục vụ ETag & If-Match)", example = "0")
        Long version,

        @Schema(description = "Thời điểm tạo đơn", example = "2026-10-25T10:00:00Z")
        Instant createdAt,

        @Schema(description = "Thời điểm cập nhật gần nhất", example = "2026-10-25T10:05:00Z")
        Instant updatedAt
) {}
