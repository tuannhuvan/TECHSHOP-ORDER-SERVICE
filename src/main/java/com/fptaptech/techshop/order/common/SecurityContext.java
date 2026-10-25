package com.fptaptech.techshop.order.common;

import java.util.Optional;

public final class SecurityContext {
    private static final ThreadLocal<UserContext> CURRENT_USER = new ThreadLocal<>();

    private SecurityContext() {}

    public static void set(UserContext user) {
        CURRENT_USER.set(user);
    }

    public static Optional<UserContext> get() {
        return Optional.ofNullable(CURRENT_USER.get());
    }

    public static UserContext requireUser() {
        return get().orElseThrow(() -> new UnauthorizedException("Yêu cầu xác thực Bearer JWT hoặc X-Partner-Key"));
    }

    public static void clear() {
        CURRENT_USER.remove();
    }
}
