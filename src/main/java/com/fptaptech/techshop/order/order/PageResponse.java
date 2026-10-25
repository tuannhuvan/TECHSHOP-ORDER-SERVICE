package com.fptaptech.techshop.order.order;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.domain.Page;

import java.util.List;

@Schema(description = "Vỏ bọc phản hồi phân trang chuẩn ổn định")
public record PageResponse<T>(
        @Schema(description = "Danh sách bản ghi của trang hiện tại")
        List<T> content,

        @Schema(description = "Thông tin siêu dữ liệu phân trang")
        PageMetadata page
) {
    public record PageMetadata(
            @Schema(description = "Số bản ghi trên mỗi trang", example = "20")
            int size,

            @Schema(description = "Chỉ số trang hiện tại (bắt đầu từ 0)", example = "0")
            int number,

            @Schema(description = "Tổng số lượng bản ghi thỏa mãn", example = "42")
            long totalElements,

            @Schema(description = "Tổng số trang", example = "3")
            int totalPages
    ) {}

    public static <T> PageResponse<T> of(Page<T> springPage) {
        return new PageResponse<>(
                springPage.getContent(),
                new PageMetadata(
                        springPage.getSize(),
                        springPage.getNumber(),
                        springPage.getTotalElements(),
                        springPage.getTotalPages()
                )
        );
    }
}
