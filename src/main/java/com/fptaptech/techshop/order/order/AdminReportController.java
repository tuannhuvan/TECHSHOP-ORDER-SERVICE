package com.fptaptech.techshop.order.order;

import com.fptaptech.techshop.order.common.ForbiddenException;
import com.fptaptech.techshop.order.common.SecurityContext;
import com.fptaptech.techshop.order.common.UserContext;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AdminReportController implements AdminReportApi {

    private final ReportService reportService;

    public AdminReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @Override
    public ResponseEntity<DailyRevenueReport> getDailyRevenueReport(String from, String to) {
        UserContext user = SecurityContext.get().orElse(null);
        if (user != null && !user.isAdmin()) {
            throw new ForbiddenException("Chỉ tài khoản ADMIN mới có quyền truy cập báo cáo doanh thu");
        }

        DailyRevenueReport report = reportService.getDailyRevenueReport(from, to);
        return ResponseEntity.ok(report);
    }
}
