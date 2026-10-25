package com.fptaptech.techshop.order.order;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

@Schema(description = "Dữ liệu yêu cầu đặt hàng mới")
public record OrderRequest(
        @Schema(description = "Mã định danh khách hàng", example = "1")
        @NotNull(message = "Mã khách hàng không được để trống")
        Long customerId,

        @Schema(description = "Địa chỉ nhận hàng", example = "Toà nhà FPT, Số 8 Tôn Thất Thuyết, Cầu Giấy, Hà Nội")
        @NotBlank(message = "Địa chỉ nhận hàng không được để trống")
        @Size(max = 500, message = "Địa chỉ tối đa 500 ký tự")
        String shippingAddress,

        @Schema(description = "Danh sách sản phẩm đặt mua (1 đến 50 dòng sản phẩm)")
        @NotEmpty(message = "Danh sách sản phẩm không được để trống")
        @Size(min = 1, max = 50, message = "Số dòng sản phẩm trong đơn từ 1 đến 50")
        List<@Valid OrderItemRequest> items,

        @Schema(description = "Thông tin phương thức thanh toán")
        @NotNull(message = "Thông tin thanh toán không được để trống")
        @Valid
        PaymentRequest payment
) {}
