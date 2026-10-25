package com.fptaptech.techshop.order.order;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Kết quả tải lên hóa đơn PDF")
public record InvoiceResponse(
        @Schema(description = "Mã đơn hàng", example = "ORD-2026-000123")
        String orderCode,

        @Schema(description = "Tên tập tin hóa đơn", example = "invoice-ORD-2026-000123.pdf")
        String fileName,

        @Schema(description = "Dung lượng tập tin (bytes)", example = "204850")
        Long fileSize,

        @Schema(description = "Thời điểm tải lên", example = "2026-10-25T11:00:00Z")
        Instant uploadedAt
) {}
