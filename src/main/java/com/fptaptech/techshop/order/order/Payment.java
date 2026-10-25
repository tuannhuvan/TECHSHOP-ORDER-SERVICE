package com.fptaptech.techshop.order.order;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Payment {

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 20)
    private PaymentMethod method;

    @Column(name = "card_provider", length = 50)
    private String cardProvider;

    @Column(name = "card_last4", length = 4)
    private String cardLast4;

    @Column(name = "bank_code", length = 20)
    private String bankCode;

    @Column(name = "transfer_ref", length = 100)
    private String transferRef;

    public static Payment cod() {
        return new Payment(PaymentMethod.COD, null, null, null, null);
    }

    public static Payment card(String provider, String last4) {
        return new Payment(PaymentMethod.CARD, provider, last4, null, null);
    }

    public static Payment bankTransfer(String bankCode, String transferRef) {
        return new Payment(PaymentMethod.BANK_TRANSFER, null, null, bankCode, transferRef);
    }
}
