package com.fptaptech.techshop.order.order;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Nhật ký kiểm toán chuyển trạng thái đơn hàng")
public record OrderEventResponse(
        @Schema(description = "ID bản ghi sự kiện", example = "1")
        Long id,

        @Schema(description = "Trạng thái trước khi chuyển", example = "PENDING")
        OrderStatus fromStatus,

        @Schema(description = "Trạng thái sau khi chuyển", example = "CONFIRMED")
        OrderStatus toStatus,

        @Schema(description = "Hành động thực thi", example = "CONFIRM")
        OrderAction action,

        @Schema(description = "Người hoặc hệ thống thực hiện", example = "ADMIN")
        String actor,

        @Schema(description = "Lý do chuyển trạng thái (bắt buộc khi hủy đơn CONFIRMED)", example = "Đã nhận thanh toán qua ngân hàng")
        String reason,

        @Schema(description = "Thời điểm ghi nhận", example = "2026-10-25T10:05:00Z")
        Instant createdAt
) {}
