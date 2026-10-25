package com.fptaptech.techshop.order.order;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "Admin Reports", description = "Báo cáo doanh thu và phân tích thống kê đơn hàng dành cho quản trị viên")
public interface AdminReportApi {

    @Operation(
            summary = "Báo cáo doanh thu đơn hàng theo ngày",
            description = "Tổng hợp doanh thu và số lượng đơn hàng theo từng ngày trong khoảng thời gian xác định (loại trừ các đơn CANCELLED). Yêu cầu quyền ADMIN.",
            security = @SecurityRequirement(name = "bearerAuth"),
            responses = {
                    @ApiResponse(responseCode = "200", description = "Báo cáo doanh thu tổng hợp thành công"),
                    @ApiResponse(responseCode = "401", description = "Chưa đăng nhập / Token thiếu"),
                    @ApiResponse(responseCode = "403", description = "Không có quyền ADMIN")
            }
    )
    @GetMapping(value = "/api/admin/reports/daily-revenue", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<DailyRevenueReport> getDailyRevenueReport(
            @Parameter(description = "Từ ngày định dạng YYYY-MM-DD", example = "2026-10-01") @RequestParam(required = false) String from,
            @Parameter(description = "Đến ngày định dạng YYYY-MM-DD", example = "2026-10-25") @RequestParam(required = false) String to
    );
}
