package com.fptaptech.techshop.order.order;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Giá trị tiền tệ đa quốc gia")
public record MoneyResponse(
        @Schema(description = "Giá trị số tiền", example = "1590000.00")
        BigDecimal value,

        @Schema(description = "Mã tiền tệ ISO 4217", example = "VND")
        String currency
) {}
