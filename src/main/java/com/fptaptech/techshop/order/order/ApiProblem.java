package com.fptaptech.techshop.order.order;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Schema(description = "Chuẩn phản hồi lỗi chi tiết RFC 9457 (ProblemDetail mở rộng)")
public record ApiProblem(
        @Schema(description = "URI nhận diện loại lỗi", example = "https://api.example.com/errors/validation")
        String type,

        @Schema(description = "Tiêu đề ngắn gọn của lỗi", example = "Validation failed")
        String title,

        @Schema(description = "Mã trạng thái HTTP tương ứng", example = "400")
        int status,

        @Schema(description = "Mã lỗi nghiệp vụ của hệ thống TechShop", example = "INVALID_INPUT")
        String code,

        @Schema(description = "Mô tả chi tiết nguyên nhân gây lỗi", example = "Dữ liệu yêu cầu không hợp lệ.")
        String detail,

        @Schema(description = "Đường dẫn endpoint phát sinh lỗi", example = "/api/orders")
        String instance,

        @Schema(description = "Thời điểm xảy ra lỗi", example = "2026-10-25T11:00:00Z")
        Instant timestamp,

        @Schema(description = "Danh sách chi tiết các trường vi phạm validation (nếu có)")
        List<Map<String, String>> errors
) {}
