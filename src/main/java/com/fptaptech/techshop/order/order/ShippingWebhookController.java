package com.fptaptech.techshop.order.order;

import com.fptaptech.techshop.order.common.SecurityContext;
import com.fptaptech.techshop.order.common.UnauthorizedException;
import com.fptaptech.techshop.order.common.UserContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
public class ShippingWebhookController implements ShippingWebhookApi {

    private final OrderService orderService;
    private final String expectedPartnerKey;

    public ShippingWebhookController(
            OrderService orderService,
            @Value("${app.partner.api-key:techshop-partner-shipping-secret-key-2026}") String expectedPartnerKey) {
        this.orderService = orderService;
        this.expectedPartnerKey = expectedPartnerKey;
    }

    @Override
    public ResponseEntity<WebhookResponse> handleShippingWebhook(String partnerKey, ShippingWebhookRequest request) {
        if (partnerKey == null || !expectedPartnerKey.equals(partnerKey)) {
            throw new UnauthorizedException("Header X-Partner-Key không chính xác hoặc thiếu");
        }

        UserContext partnerActor = new UserContext(888L, "partner@carrier.vn", UserContext.ROLE_PARTNER, partnerKey);

        Order order = orderService.getOrderEntityByCode(request.orderCode());
        // For partner webhooks, automatically use current version to guarantee optimistic locking
        String ifMatch = String.valueOf(order.getVersion());

        TransitionRequest transitionRequest = new TransitionRequest(
                request.action(),
                request.reason() != null ? request.reason() : request.partnerNotes()
        );

        OrderResponseV2 updated = orderService.transition(request.orderCode(), transitionRequest, ifMatch, partnerActor);

        return ResponseEntity.ok(new WebhookResponse(
                true,
                updated.code(),
                updated.status(),
                "Cập nhật trạng thái từ đối tác vận chuyển thành công: " + updated.status(),
                Instant.now()
        ));
    }
}
