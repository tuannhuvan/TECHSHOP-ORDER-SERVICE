package com.fptaptech.techshop.order.order;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Yêu cầu dòng sản phẩm trong đơn hàng")
public record OrderItemRequest(
        @Schema(description = "Mã định danh sản phẩm", example = "1")
        @NotNull(message = "Mã sản phẩm không được để trống")
        Long productId,

        @Schema(description = "Số lượng đặt hàng (1 đến 20)", example = "2")
        @NotNull(message = "Số lượng không được để trống")
        @Min(value = 1, message = "Số lượng phải từ 1 đến 20")
        @Max(value = 20, message = "Số lượng phải từ 1 đến 20")
        Integer quantity
) {}
