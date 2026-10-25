# ARCHITECTURE MAP: TECHSHOP ORDER SERVICE

---

## 1. CẤU TRÚC GÓI MÃ NGUỒN (PACKAGE STRUCTURE)
```
com.fptaptech.techshop.order/
├── OrderApplication.java
├── config/
│   ├── OpenApiConfig.java          # Cấu hình 4 nhóm Spec & Security Schemes
│   └── WebSecurityConfig.java       # OAuth2 Resource Server / JWT
├── common/
│   ├── GlobalExceptionHandler.java  # Handler RFC 9457 ProblemDetail
│   └── IdempotencyFilter.java      # Filter kiểm tra Idempotency-Key
└── order/
    ├── Order.java                   # JPA Entity (có @Version)
    ├── OrderItem.java               # JPA Entity
    ├── Payment.java                # Embeddable / Polymorphic Payment
    ├── OrderEvent.java              # JPA Entity
    ├── OrderRequest.java            # DTO record vào
    ├── OrderResponseV1.java         # DTO record ra (v1)
    ├── OrderResponseV2.java         # DTO record ra (v2)
    ├── OrderMapper.java             # MapStruct Mapper
    ├── OrderRepository.java         # JpaRepository
    ├── OrderService.java            # Business logic & State machine
    ├── OrderApi.java                # Interface OpenAPI annotations
    └── OrderController.java         # REST Controller
```

---

## 2. LUỒNG XỬ LÝ CHÍNH (DATA & LOGIC FLOW)
1. **Client Request** -> `IdempotencyFilter` (Kiểm tra Header `Idempotency-Key` trong Redis/DB).
2. **Security Context** -> Xác thực JWT Bearer hoặc `X-Partner-Key`.
3. **Controller Layer** -> Nhận DTO Record + Thực thi Validation (`@Valid`).
4. **Service Layer** -> Chuyển đổi qua MapStruct, tính toán giá server-side, cập nhật máy trạng thái, kiểm tra `If-Match` version.
5. **Persistence Layer** -> Lưu JPA Entity `Order` + `OrderEvent` vào MySQL 8.4.
