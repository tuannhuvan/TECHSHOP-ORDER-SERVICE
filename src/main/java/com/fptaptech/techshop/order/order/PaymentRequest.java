package com.fptaptech.techshop.order.order;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Thông tin phương thức thanh toán đa hình (COD, CARD, BANK_TRANSFER)")
public record PaymentRequest(
        @Schema(description = "Phương thức thanh toán", example = "COD", allowableValues = {"COD", "CARD", "BANK_TRANSFER"})
        @NotNull(message = "Phương thức thanh toán không được để trống")
        PaymentMethod method,

        @Schema(description = "Đơn vị phát hành thẻ (áp dụng cho CARD)", example = "Visa")
        @Size(max = 50, message = "Tên đơn vị thẻ tối đa 50 ký tự")
        String cardProvider,

        @Schema(description = "4 số cuối của thẻ (áp dụng cho CARD)", example = "4242")
        @Pattern(regexp = "^$|^[0-9]{4}$", message = "4 số cuối thẻ phải là 4 chữ số")
        String cardLast4,

        @Schema(description = "Mã ngân hàng (áp dụng cho BANK_TRANSFER)", example = "VCB")
        @Size(max = 20, message = "Mã ngân hàng tối đa 20 ký tự")
        String bankCode,

        @Schema(description = "Mã tham chiếu chuyển khoản (áp dụng cho BANK_TRANSFER)", example = "REF-20261025-001")
        @Size(max = 100, message = "Mã tham chiếu tối đa 100 ký tự")
        String transferRef
) {
    public static PaymentRequest cod() {
        return new PaymentRequest(PaymentMethod.COD, null, null, null, null);
    }

    public static PaymentRequest card(String provider, String last4) {
        return new PaymentRequest(PaymentMethod.CARD, provider, last4, null, null);
    }

    public static PaymentRequest bankTransfer(String bankCode, String transferRef) {
        return new PaymentRequest(PaymentMethod.BANK_TRANSFER, null, null, bankCode, transferRef);
    }
}
