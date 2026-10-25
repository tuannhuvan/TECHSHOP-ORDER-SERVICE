# PROJECT BRIEF: TECHSHOP ORDER SERVICE
**Mô tả:** Microservice quản lý toàn bộ quy trình đặt hàng, thanh toán, vận chuyển và báo cáo doanh thu cho hệ thống thương mại điện tử TechShop.

---

## 1. MỤC TIÊU DỰ ÁN
- Triển khai Order Service chuẩn REST API dựa trên Spring Boot 4.1 & JDK 25.
- Tự động hóa tài liệu OpenAPI 3.1 & Swagger UI với 4 nhóm spec riêng biệt (`public-v1`, `public-v2`, `admin`, `partner`).
- Hỗ trợ xử lý lỗi chuẩn RFC 9457 (`ProblemDetail`).
- Đảm bảo tính Idempotent khi tạo đơn và Optimistic Locking (`If-Match`) khi chuyển trạng thái.

## 2. KỸ THUẬT & CÔNG CỤ (STACK)
- **Language:** Java 25 (LTS)
- **Framework:** Spring Boot 4.1.1 (Spring Framework 7, Jakarta EE 11)
- **Documentation:** springdoc-openapi-starter-webmvc-ui 3.1.1
- **Object Mapping:** MapStruct 1.6.3
- **Database:** MySQL 8.4 LTS (Docker Compose support) / H2 for Testing
- **Build Tool:** Maven 3.9+ (`mvnw`)
