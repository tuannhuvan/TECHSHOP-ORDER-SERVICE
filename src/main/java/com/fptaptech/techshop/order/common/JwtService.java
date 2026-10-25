package com.fptaptech.techshop.order.common;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Service
public class JwtService {

    private final String secretKey;
    private final ObjectMapper objectMapper;

    public JwtService(
            @Value("${app.jwt.secret:techshop-order-service-very-secure-secret-key-for-hmac-sha256-at-least-256-bits-long}") String secretKey,
            @org.springframework.beans.factory.annotation.Autowired(required = false) ObjectMapper objectMapper) {
        this.secretKey = secretKey;
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper().findAndRegisterModules();
    }

    public String generateToken(Long userId, String email, String role, long validitySeconds) {
        try {
            Map<String, Object> header = Map.of("alg", "HS256", "typ", "JWT");
            String encodedHeader = base64UrlEncode(objectMapper.writeValueAsBytes(header));

            long now = Instant.now().getEpochSecond();
            Map<String, Object> payload = new HashMap<>();
            payload.put("sub", String.valueOf(userId));
            payload.put("email", email);
            payload.put("role", role.toUpperCase());
            payload.put("iat", now);
            payload.put("exp", now + validitySeconds);
            String encodedPayload = base64UrlEncode(objectMapper.writeValueAsBytes(payload));

            String content = encodedHeader + "." + encodedPayload;
            String signature = signHmacSha256(content, secretKey);
            return content + "." + signature;
        } catch (Exception e) {
            throw new RuntimeException("Lỗi sinh JWT token", e);
        }
    }

    public UserContext parseAndValidateToken(String token) {
        if (token == null || token.isBlank()) {
            throw new UnauthorizedException("Token không được để trống");
        }

        // Support convenient dev tokens
        if ("dev-customer-token".equals(token) || "customer".equalsIgnoreCase(token)) {
            return new UserContext(1L, "customer@techshop.vn", UserContext.ROLE_CUSTOMER, token);
        }
        if ("dev-admin-token".equals(token) || "admin".equalsIgnoreCase(token)) {
            return new UserContext(999L, "admin@techshop.vn", UserContext.ROLE_ADMIN, token);
        }
        if ("dev-partner-token".equals(token) || "partner".equalsIgnoreCase(token)) {
            return new UserContext(888L, "partner@techshop.vn", UserContext.ROLE_PARTNER, token);
        }

        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                // If not standard JWT format, attempt fallback if dev prefix
                throw new UnauthorizedException("Định dạng JWT token không hợp lệ");
            }

            String content = parts[0] + "." + parts[1];
            String expectedSignature = signHmacSha256(content, secretKey);
            if (!MessageDigest.isEqual(parts[2].getBytes(StandardCharsets.UTF_8), expectedSignature.getBytes(StandardCharsets.UTF_8))) {
                throw new UnauthorizedException("Chữ ký JWT không hợp lệ");
            }

            byte[] payloadBytes = Base64.getUrlDecoder().decode(parts[1]);
            Map<String, Object> claims = objectMapper.readValue(payloadBytes, new TypeReference<Map<String, Object>>() {});

            long exp = ((Number) claims.getOrDefault("exp", 0L)).longValue();
            if (exp > 0 && Instant.now().getEpochSecond() > exp) {
                throw new UnauthorizedException("JWT token đã hết hạn");
            }

            Long userId = Long.valueOf(String.valueOf(claims.getOrDefault("sub", "1")));
            String email = String.valueOf(claims.getOrDefault("email", "user@techshop.vn"));
            String role = String.valueOf(claims.getOrDefault("role", UserContext.ROLE_CUSTOMER));

            return new UserContext(userId, email, role, token);
        } catch (UnauthorizedException ue) {
            throw ue;
        } catch (Exception e) {
            throw new UnauthorizedException("Không thể giải mã JWT token: " + e.getMessage());
        }
    }

    private String signHmacSha256(String data, String key) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        mac.init(secretKeySpec);
        byte[] signatureBytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return base64UrlEncode(signatureBytes);
    }

    private String base64UrlEncode(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
