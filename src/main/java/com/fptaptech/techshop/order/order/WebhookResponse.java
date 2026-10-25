package com.fptaptech.techshop.order.order;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Kết quả phản hồi xử lý webhook vận chuyển")
public record WebhookResponse(
        @Schema(description = "Trạng thái xử lý thành công", example = "true")
        boolean success,

        @Schema(description = "Mã đơn hàng", example = "ORD-2026-000123")
        String orderCode,

        @Schema(description = "Trạng thái mới của đơn sau khi cập nhật", example = "DELIVERED")
        OrderStatus status,

        @Schema(description = "Thông báo kết quả", example = "Đã cập nhật trạng thái đơn hàng thành công")
        String message,

        @Schema(description = "Thời điểm xử lý", example = "2026-10-25T11:30:00Z")
        Instant processedAt
) {}
