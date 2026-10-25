package com.fptaptech.techshop.order.order;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

@Schema(description = "Thông tin chi tiết đơn hàng (Public Spec V2 - Chuẩn hóa tiền tệ đa quốc gia)")
public record OrderResponseV2(
        @Schema(description = "ID nội bộ", example = "1")
        Long id,

        @Schema(description = "Mã đơn hàng định dạng nghiệp vụ", example = "ORD-2026-000123")
        String code,

        @Schema(description = "ID khách hàng", example = "1")
        Long customerId,

        @Schema(description = "Trạng thái đơn hàng", example = "CONFIRMED")
        OrderStatus status,

        @Schema(description = "Địa chỉ nhận hàng", example = "Toà nhà FPT, Cầu Giấy, Hà Nội")
        String shippingAddress,

        @Schema(description = "Tổng tiền đơn hàng kèm đơn vị tiền tệ")
        MoneyResponse total,

        @Schema(description = "Thông tin thanh toán")
        PaymentResponse payment,

        @Schema(description = "Danh sách sản phẩm trong đơn")
        List<OrderItemResponse> items,

        @Schema(description = "Phiên bản đơn hàng (phục vụ ETag & If-Match)", example = "0")
        Long version,

        @Schema(description = "Thời điểm giao hàng thành công (nếu có)")
        Instant deliveredAt,

        @Schema(description = "Thời điểm tạo đơn", example = "2026-10-25T10:00:00Z")
        Instant createdAt,

        @Schema(description = "Thời điểm cập nhật gần nhất", example = "2026-10-25T10:05:00Z")
        Instant updatedAt
) {}
