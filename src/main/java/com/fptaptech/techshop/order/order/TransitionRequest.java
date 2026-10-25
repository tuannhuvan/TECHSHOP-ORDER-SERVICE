package com.fptaptech.techshop.order.order;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Yêu cầu thực hiện hành động chuyển trạng thái đơn hàng")
public record TransitionRequest(
        @Schema(
                description = "Hành động chuyển trạng thái được phép theo ma trận phân quyền: CONFIRM (ADMIN), CANCEL (CUSTOMER/ADMIN), SHIP (ADMIN/PARTNER), DELIVER (PARTNER), FAIL_DELIVERY (PARTNER), REQUEST_RETURN (CUSTOMER <= 7 ngày), ACCEPT_RETURN (ADMIN)",
                example = "CONFIRM",
                allowableValues = {"CONFIRM", "CANCEL", "SHIP", "DELIVER", "FAIL_DELIVERY", "REQUEST_RETURN", "ACCEPT_RETURN"}
        )
        @NotNull(message = "Hành động không được để trống")
        OrderAction action,

        @Schema(
                description = "Lý do chuyển trạng thái (bắt buộc đối với hành động CANCEL khi đơn ở trạng thái CONFIRMED)",
                example = "Khách hàng đổi ý mua sản phẩm khác"
        )
        @Size(max = 500, message = "Lý do tối đa 500 ký tự")
        String reason
) {}
