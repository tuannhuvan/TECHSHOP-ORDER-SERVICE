package com.fptaptech.techshop.order.order;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Tag(name = "Orders", description = "Quản lý vòng đời đơn hàng, chuyển trạng thái và hóa đơn")
public interface OrderApi {

    @Operation(
            summary = "Tạo đơn hàng mới",
            description = "Tạo đơn hàng mới cho khách hàng. Yêu cầu Header Idempotency-Key để bảo đảm tính duy nhất và tính giá phía server (BR-01, BR-02, BR-03).",
            security = @SecurityRequirement(name = "bearerAuth"),
            parameters = {
                    @Parameter(
                            name = "Idempotency-Key",
                            in = ParameterIn.HEADER,
                            required = true,
                            description = "Khóa Idempotency duy nhất đại diện cho giao dịch tạo đơn (ví dụ: UUID)",
                            example = "9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d"
                    )
            },
            requestBody = @RequestBody(
                    description = "Dữ liệu tạo đơn hàng",
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = OrderRequest.class),
                            examples = {
                                    @ExampleObject(
                                            name = "1. Đặt hàng thanh toán COD",
                                            summary = "Đơn hàng COD thông thường",
                                            value = """
                                                    {
                                                      "customerId": 1,
                                                      "shippingAddress": "Số 8 Tôn Thất Thuyết, Cầu Giấy, Hà Nội",
                                                      "items": [
                                                        { "productId": 1, "quantity": 1 },
                                                        { "productId": 2, "quantity": 1 }
                                                      ],
                                                      "payment": {
                                                        "method": "COD"
                                                      }
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "2. Đặt hàng thanh toán Thẻ CARD",
                                            summary = "Đơn hàng thanh toán qua Thẻ tín dụng/ghi nợ",
                                            value = """
                                                    {
                                                      "customerId": 1,
                                                      "shippingAddress": "Toà Landmark 81, Bình Thạnh, TP.HCM",
                                                      "items": [
                                                        { "productId": 3, "quantity": 2 }
                                                      ],
                                                      "payment": {
                                                        "method": "CARD",
                                                        "cardProvider": "Visa",
                                                        "cardLast4": "4242"
                                                      }
                                                    }
                                                    """
                                    )
                            }
                    )
            ),
            responses = {
                    @ApiResponse(
                            responseCode = "201",
                            description = "Đơn hàng đã được tạo thành công",
                            headers = @Header(name = "Location", description = "URI trỏ tới tài nguyên đơn hàng vừa tạo"),
                            content = @Content(schema = @Schema(implementation = OrderResponseV2.class))
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Dữ liệu đầu vào vi phạm validation hoặc trùng productId",
                            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ApiProblem.class))
                    ),
                    @ApiResponse(
                            responseCode = "409",
                            description = "Xung đột Idempotency Key (cùng key nhưng khác nội dung body)",
                            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ApiProblem.class))
                    )
            }
    )
    @PostMapping(value = "/api/orders", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<OrderResponseV2> createOrder(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @org.springframework.web.bind.annotation.RequestBody OrderRequest request
    );

    @Operation(
            summary = "Tra cứu và phân trang danh sách đơn hàng",
            description = "Lấy danh sách đơn hàng theo bộ lọc phân trang, trạng thái, khoảng thời gian và tổng tiền (D4 pagination envelope). Khách hàng chỉ xem được đơn của mình.",
            security = @SecurityRequirement(name = "bearerAuth"),
            responses = {
                    @ApiResponse(responseCode = "200", description = "Truy vấn danh sách đơn hàng thành công"),
                    @ApiResponse(responseCode = "401", description = "Chưa xác thực hoặc token không hợp lệ")
            }
    )
    @GetMapping(value = "/api/orders", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<PageResponse<OrderResponseV2>> getOrders(
            @Parameter(description = "Lọc theo trạng thái đơn hàng") @RequestParam(required = false) OrderStatus status,
            @Parameter(description = "Từ ngày (ISO Instant)") @RequestParam(required = false) Instant from,
            @Parameter(description = "Đến ngày (ISO Instant)") @RequestParam(required = false) Instant to,
            @Parameter(description = "Tổng tiền tối thiểu (VND)") @RequestParam(required = false) BigDecimal minTotal,
            @Parameter(description = "Chỉ số trang (0-indexed)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Số lượng bản ghi mỗi trang") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Trường sắp xếp (createdAt, totalAmount, status)") @RequestParam(defaultValue = "createdAt,desc") String sort
    );

    @Operation(
            summary = "Xem chi tiết đơn hàng (Spec v2 chuẩn)",
            description = "Trả về thông tin chi tiết đơn hàng với trường tổng tiền chuẩn đa tiền tệ `total: {value, currency}`.",
            security = @SecurityRequirement(name = "bearerAuth"),
            responses = {
                    @ApiResponse(responseCode = "200", description = "Tìm thấy đơn hàng"),
                    @ApiResponse(responseCode = "404", description = "Đơn hàng không tồn tại", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ApiProblem.class)))
            }
    )
    @GetMapping(value = "/api/orders/{code}", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<OrderResponseV2> getOrderByCode(
            @Parameter(description = "Mã đơn hàng", example = "ORD-2026-000101") @PathVariable String code
    );

    @Operation(
            summary = "Xem chi tiết đơn hàng (Spec v1 - Deprecated)",
            description = "Endpoint kế thừa phiên bản cũ v1 trả về trường `totalAmount` dạng số. Được đánh dấu Deprecated.",
            deprecated = true,
            security = @SecurityRequirement(name = "bearerAuth"),
            responses = {
                    @ApiResponse(responseCode = "200", description = "Tìm thấy đơn hàng dạng v1"),
                    @ApiResponse(responseCode = "404", description = "Đơn hàng không tồn tại")
            }
    )
    @GetMapping(value = "/api/v1/orders/{code}", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<OrderResponseV1> getOrderByCodeV1(
            @Parameter(description = "Mã đơn hàng", example = "ORD-2026-000101") @PathVariable String code
    );

    @Operation(
            summary = "Thực hiện hành động chuyển trạng thái đơn hàng",
            description = "Thực thi chuyển trạng thái theo máy trạng thái 7 trạng thái, 8 hành động. Yêu cầu Header If-Match mang version hiện tại của đơn hàng (Optimistic Locking BR-06).",
            security = @SecurityRequirement(name = "bearerAuth"),
            parameters = {
                    @Parameter(
                            name = "If-Match",
                            in = ParameterIn.HEADER,
                            required = true,
                            description = "Version hiện tại của đơn hàng từ ETag (ví dụ: \"0\" hoặc \"1\")",
                            example = "\"0\""
                    )
            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "Chuyển trạng thái đơn hàng thành công"),
                    @ApiResponse(responseCode = "400", description = "Lý do thiếu khi hủy đơn CONFIRMED hoặc vi phạm điều kiện 7 ngày trả hàng"),
                    @ApiResponse(responseCode = "409", description = "Chuyển trạng thái không hợp lệ (mã lỗi ORDER_INVALID_TRANSITION)"),
                    @ApiResponse(responseCode = "412", description = "Header If-Match không khớp với version hiện tại (Precondition Failed)")
            }
    )
    @PostMapping(value = "/api/orders/{code}/transitions", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<OrderResponseV2> transitionOrder(
            @Parameter(description = "Mã đơn hàng", example = "ORD-2026-000101") @PathVariable String code,
            @RequestHeader(value = "If-Match", required = false) String ifMatch,
            @Valid @org.springframework.web.bind.annotation.RequestBody TransitionRequest request
    );

    @Operation(
            summary = "Tải lên hóa đơn bán lẻ PDF (Admin)",
            description = "Quản trị viên tải lên file PDF hóa đơn kèm theo đơn hàng (dung lượng <= 5MB).",
            security = @SecurityRequirement(name = "bearerAuth"),
            responses = {
                    @ApiResponse(responseCode = "200", description = "Tải lên hóa đơn thành công"),
                    @ApiResponse(responseCode = "400", description = "Tệp không hợp lệ hoặc vượt quá 5MB"),
                    @ApiResponse(responseCode = "404", description = "Không tìm thấy đơn hàng")
            }
    )
    @PostMapping(value = "/api/orders/{code}/invoice", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<InvoiceResponse> uploadInvoice(
            @Parameter(description = "Mã đơn hàng", example = "ORD-2026-000101") @PathVariable String code,
            @RequestParam("file") MultipartFile file
    );

    @Operation(
            summary = "Tải về hóa đơn bán lẻ PDF",
            description = "Khách hàng hoặc quản trị viên tải về bản PDF hóa đơn của đơn hàng.",
            security = @SecurityRequirement(name = "bearerAuth"),
            responses = {
                    @ApiResponse(responseCode = "200", description = "File PDF hóa đơn", content = @Content(mediaType = "application/pdf")),
                    @ApiResponse(responseCode = "404", description = "Không tìm thấy đơn hàng")
            }
    )
    @GetMapping(value = "/api/orders/{code}/invoice", produces = MediaType.APPLICATION_PDF_VALUE)
    ResponseEntity<byte[]> getInvoicePdf(
            @Parameter(description = "Mã đơn hàng", example = "ORD-2026-000101") @PathVariable String code
    );

    @Operation(
            summary = "Xem lịch sử nhật ký kiểm toán (Audit Trail) của đơn hàng",
            description = "Xem danh sách các sự kiện chuyển trạng thái trong suốt vòng đời của đơn hàng.",
            security = @SecurityRequirement(name = "bearerAuth"),
            responses = {
                    @ApiResponse(responseCode = "200", description = "Danh sách sự kiện chuyển trạng thái")
            }
    )
    @GetMapping(value = "/api/orders/{code}/events", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<List<OrderEventResponse>> getOrderEvents(
            @Parameter(description = "Mã đơn hàng", example = "ORD-2026-000101") @PathVariable String code
    );
}
