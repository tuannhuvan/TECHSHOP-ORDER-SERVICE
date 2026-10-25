package com.fptaptech.techshop.order.common;

public record UserContext(
        Long userId,
        String email,
        String role,
        String token
) {
    public static final String ROLE_CUSTOMER = "CUSTOMER";
    public static final String ROLE_ADMIN = "ADMIN";
    public static final String ROLE_PARTNER = "PARTNER";

    public boolean isCustomer() {
        return ROLE_CUSTOMER.equalsIgnoreCase(role);
    }

    public boolean isAdmin() {
        return ROLE_ADMIN.equalsIgnoreCase(role);
    }

    public boolean isPartner() {
        return ROLE_PARTNER.equalsIgnoreCase(role);
    }
}
