package com.fptaptech.techshop.order.order;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@Tag(name = "Shipping Partner", description = "Webhook tiếp nhận thông tin từ các đối tác giao hàng")
public interface ShippingWebhookApi {

    @Operation(
            summary = "Webhook cập nhật trạng thái giao hàng",
            description = "Đối tác vận chuyển (Viettel Post, GHTK, GHN...) gọi Webhook để cập nhật tình trạng giao hàng (SHIP, DELIVER, FAIL_DELIVERY). Xác thực bằng Header X-Partner-Key.",
            security = @SecurityRequirement(name = "partnerKey"),
            parameters = {
                    @Parameter(
                            name = "X-Partner-Key",
                            in = ParameterIn.HEADER,
                            required = true,
                            description = "Khóa bí mật do TechShop cấp cho đối tác",
                            example = "techshop-partner-shipping-secret-key-2026"
                    )
            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "Đã cập nhật trạng thái thành công"),
                    @ApiResponse(responseCode = "401", description = "Header X-Partner-Key không chính xác"),
                    @ApiResponse(responseCode = "409", description = "Trạng thái đơn hàng không phù hợp với hành động vận chuyển")
            }
    )
    @PostMapping(value = "/api/webhooks/shipping", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<WebhookResponse> handleShippingWebhook(
            @RequestHeader(value = "X-Partner-Key", required = false) String partnerKey,
            @Valid @RequestBody ShippingWebhookRequest request
    );
}
