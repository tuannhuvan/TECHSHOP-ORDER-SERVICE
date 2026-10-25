package com.fptaptech.techshop.order.order;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Chi tiết mặt hàng trong đơn hàng")
public record OrderItemResponse(
        @Schema(description = "Mã dòng chi tiết", example = "1")
        Long id,

        @Schema(description = "Mã sản phẩm", example = "1")
        Long productId,

        @Schema(description = "Tên sản phẩm snapshot tại thời điểm đặt", example = "Bàn phím cơ K8 Pro")
        String productName,

        @Schema(description = "Đơn giá snapshot tại thời điểm đặt (VND)", example = "1590000.00")
        BigDecimal unitPrice,

        @Schema(description = "Số lượng mua", example = "1")
        Integer quantity,

        @Schema(description = "Thành tiền của dòng sản phẩm (VND)", example = "1590000.00")
        BigDecimal lineTotal
) {}
