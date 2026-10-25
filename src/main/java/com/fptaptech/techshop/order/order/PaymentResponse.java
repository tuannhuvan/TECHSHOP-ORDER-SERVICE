package com.fptaptech.techshop.order.order;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Thông tin thanh toán của đơn hàng")
public record PaymentResponse(
        @Schema(description = "Phương thức thanh toán", example = "COD")
        PaymentMethod method,

        @Schema(description = "Đơn vị cung cấp thẻ", example = "Visa")
        String cardProvider,

        @Schema(description = "4 chữ số cuối của thẻ", example = "4242")
        String cardLast4,

        @Schema(description = "Mã ngân hàng", example = "VCB")
        String bankCode,

        @Schema(description = "Mã tham chiếu giao dịch", example = "REF-20261025-001")
        String transferRef
) {}
