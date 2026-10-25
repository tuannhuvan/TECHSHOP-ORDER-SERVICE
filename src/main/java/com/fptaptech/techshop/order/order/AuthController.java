package com.fptaptech.techshop.order.order;

import com.fptaptech.techshop.order.common.JwtService;
import com.fptaptech.techshop.order.common.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Tag(name = "Authentication", description = "Công cụ sinh token Bearer JWT phục vụ thử nghiệm Swagger UI và ứng dụng")
@RestController
public class AuthController {

    private final JwtService jwtService;

    public AuthController(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Operation(summary = "Sinh token Bearer JWT cho mục đích kiểm thử")
    @PostMapping("/api/auth/token")
    public ResponseEntity<Map<String, Object>> generateToken(
            @Parameter(description = "Vai trò: CUSTOMER hoặc ADMIN", example = "CUSTOMER")
            @RequestParam(defaultValue = "CUSTOMER") String role,
            @Parameter(description = "Email người dùng", example = "customer@techshop.vn")
            @RequestParam(defaultValue = "customer@techshop.vn") String email,
            @Parameter(description = "ID người dùng", example = "1")
            @RequestParam(defaultValue = "1") Long userId) {

        long validity = 86400; // 24 hours
        String token = jwtService.generateToken(userId, email, role.toUpperCase(), validity);

        return ResponseEntity.ok(Map.of(
                "token", token,
                "tokenType", "Bearer",
                "role", role.toUpperCase(),
                "email", email,
                "userId", userId,
                "expiresIn", validity
        ));
    }
}
