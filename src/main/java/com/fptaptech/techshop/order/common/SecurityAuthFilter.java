package com.fptaptech.techshop.order.common;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
public class SecurityAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final String partnerApiKey;

    public SecurityAuthFilter(
            JwtService jwtService,
            @Value("${app.partner.api-key:techshop-partner-shipping-secret-key-2026}") String partnerApiKey) {
        this.jwtService = jwtService;
        this.partnerApiKey = partnerApiKey;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        // D5: Add X-Request-Id to every response
        String requestId = request.getHeader("X-Request-Id");
        if (requestId == null || requestId.isBlank()) {
            requestId = UUID.randomUUID().toString();
        }
        response.setHeader("X-Request-Id", requestId);

        try {
            // Check Partner API Key
            String partnerKey = request.getHeader("X-Partner-Key");
            if (partnerKey != null && !partnerKey.isBlank()) {
                if (partnerApiKey.equals(partnerKey)) {
                    SecurityContext.set(new UserContext(888L, "partner@techshop.carrier", UserContext.ROLE_PARTNER, partnerKey));
                }
            }

            // Check Bearer Token
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7).trim();
                try {
                    UserContext user = jwtService.parseAndValidateToken(token);
                    SecurityContext.set(user);
                } catch (Exception ignored) {
                    // Security context remains empty, endpoint security check will fail when accessed
                }
            }

            filterChain.doFilter(request, response);
        } finally {
            SecurityContext.clear();
        }
    }
}
