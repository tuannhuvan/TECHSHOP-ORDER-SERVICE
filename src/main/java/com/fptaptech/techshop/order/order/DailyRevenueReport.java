package com.fptaptech.techshop.order.order;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;

@Schema(description = "Báo cáo doanh thu theo ngày của TechShop")
public record DailyRevenueReport(
        @Schema(description = "Từ ngày (YYYY-MM-DD)", example = "2026-10-01")
        String from,

        @Schema(description = "Đến ngày (YYYY-MM-DD)", example = "2026-10-25")
        String to,

        @Schema(description = "Tổng doanh thu trong khoảng thời gian (VND)", example = "58490000.00")
        BigDecimal totalRevenue,

        @Schema(description = "Tổng số lượng đơn hàng phát sinh", example = "24")
        Long totalOrders,

        @Schema(description = "Chi tiết doanh thu từng ngày")
        List<DailyRevenueItem> items
) {
    public record DailyRevenueItem(
            @Schema(description = "Ngày thống kê (YYYY-MM-DD)", example = "2026-10-25")
            String date,

            @Schema(description = "Số lượng đơn trong ngày", example = "5")
            Long orderCount,

            @Schema(description = "Doanh thu trong ngày (VND)", example = "12500000.00")
            BigDecimal revenue
    ) {}
}
