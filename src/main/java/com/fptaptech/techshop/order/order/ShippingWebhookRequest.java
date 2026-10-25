package com.fptaptech.techshop.order.order;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Payload cập nhật tình trạng giao hàng từ đối tác vận chuyển")
public record ShippingWebhookRequest(
        @Schema(description = "Mã đơn hàng", example = "ORD-2026-000123")
        @NotBlank(message = "Mã đơn hàng không được để trống")
        String orderCode,

        @Schema(
                description = "Hành động vận chuyển từ đối tác (SHIP, DELIVER, FAIL_DELIVERY)",
                example = "DELIVER",
                allowableValues = {"SHIP", "DELIVER", "FAIL_DELIVERY"}
        )
        @NotNull(message = "Hành động không được để trống")
        OrderAction action,

        @Schema(description = "Lý do cập nhật (áp dụng khi FAIL_DELIVERY)", example = "Không liên lạc được với khách hàng sau 3 lần gọi")
        @Size(max = 500, message = "Lý do tối đa 500 ký tự")
        String reason,

        @Schema(description = "Ghi chú từ đối tác vận chuyển", example = "Bưu tá: Nguyễn Văn A - SĐT: 0912345678")
        @Size(max = 500, message = "Ghi chú tối đa 500 ký tự")
        String partnerNotes
) {}
