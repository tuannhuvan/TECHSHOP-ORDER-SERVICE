# 🛍️ TECHSHOP ORDER SERVICE

> **Microservice quản lý toàn bộ quy trình đặt hàng, thanh toán, máy trạng thái, vận chuyển và báo cáo doanh thu cho nền tảng thương mại điện tử TechShop.**

[![Java 25 LTS](https://img.shields.io/badge/Java-25%20LTS-orange.svg)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1.2-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![OpenAPI](https://img.shields.io/badge/OpenAPI-3.1.0-blue.svg)](https://swagger.io/)
[![RFC 9457](https://img.shields.io/badge/RFC-9457%20ProblemDetail-blueviolet.svg)](https://datatracker.ietf.org/doc/html/rfc9457)
[![Tests](https://img.shields.io/badge/Tests-25%20Passed-success.svg)]()

---

## ⚡ 1. HƯỚNG DẪN KHỞI CHẠY BẰNG MỘT LỆNH (ONE-LINE RUN)

Hệ thống được cấu hình sẵn cơ sở dữ liệu nhúng H2 (chế độ MySQL compatibility) cùng bộ nạp dữ liệu mẫu ban đầu, giúp bạn **khởi chạy ngay lập tức bằng đúng 1 lệnh duy nhất** mà không cần cài đặt thêm MySQL hay Docker bên ngoài:

### Trên Windows (PowerShell / Command Prompt):
```powershell
.\mvnw.cmd spring-boot:run
```

### Trên Linux / macOS:
```bash
./mvnw spring-boot:run
```

### Chạy toàn bộ bộ test kiểm thử tự động (25 tests - 100% Passed):
```powershell
.\mvnw.cmd test
```
* **OpenApiSpecTest (5 tests):** Kiểm thử hợp đồng đặc tả OpenAPI 3.1 cho từng nhóm (`public-v1`, `public-v2`, `admin`, `partner`), kiểm tra deprecated v1 và cô lập phạm vi bảo mật.
* **OrderStateMachineTest (13 tests):** Kiểm thử toàn bộ 8 cặp chuyển hợp lệ, kiểm thử tự động **tất cả 41 cặp chuyển trạng thái không hợp lệ** trên 7 trạng thái, cùng các ràng buộc biên BR-04, BR-05 và phân quyền vai trò.
* **IdempotencyAndOptimisticLockingTest (6 tests):** Kiểm thử cơ chế Idempotency-Key (replay trả về cache, khác payload ném 409 Conflict, thiếu header trả về RFC 9457 ProblemDetail 400), kiểm thử trùng sản phẩm BR-02, và kiểm thử khóa lạc quan ETag `If-Match` BR-06 (412 Precondition Failed).
* **DemoTechShopOrderServiceApplicationTests (1 test):** Kiểm thử khởi tạo ngữ cảnh ứng dụng (Spring Context Load).

### (Tùy chọn) Chạy với container MySQL 8.4 bằng Docker Compose:
```bash
docker compose up -d && ./mvnw spring-boot:run -Dspring.docker.compose.enabled=true
```

---

## 🌐 2. ĐỊA CHỈ TRUY CẬP CÁC DỊCH VỤ

Khi ứng dụng đã khởi động thành công trên cổng `8080`:

| Dịch vụ | Địa chỉ URL | Mô tả chức năng |
| :--- | :--- | :--- |
| 🎨 **Giao diện Web Frontend** | **[http://localhost:8080/](http://localhost:8080/)** | Dashboard giao diện tươi sáng trẻ trung: mua hàng, quản lý đơn, máy trạng thái, webhook đối tác, hóa đơn PDF và báo cáo |
| 📚 **Swagger UI (4 Spec Groups)** | **[http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)** | Tài liệu tương tác OpenAPI 3.1 với dropdown 4 nhóm (`public-v1`, `public-v2`, `admin`, `partner`) |
| 📑 **OpenAPI Spec (JSON)** | `http://localhost:8080/v3/api-docs` | Đặc tả OpenAPI 3.1.0 đầy đủ |
| 🗄️ **H2 Database Console** | `http://localhost:8080/h2-console` | JDBC URL: `jdbc:h2:mem:techshopdb` &bull; User: `sa` &bull; Password: `""` |

---

## 🔑 3. THÔNG TIN XÁC THỰC MẪU (TEST CREDENTIALS)

* **Khách hàng (CUSTOMER):**
  * Token: `dev-customer-token` (hoặc sinh qua `POST /api/auth/token?role=CUSTOMER`)
  * Header: `Authorization: Bearer dev-customer-token`
* **Quản trị viên (ADMIN):**
  * Token: `dev-admin-token` (hoặc sinh qua `POST /api/auth/token?role=ADMIN`)
  * Header: `Authorization: Bearer dev-admin-token`
* **Đối tác giao vận (PARTNER):**
  * Header: `X-Partner-Key: techshop-partner-shipping-secret-key-2026`

---

## 🏗️ 4. KIẾN TRÚC & CÔNG NGHỆ ÁP DỤNG

* **Ngôn ngữ:** Java 25 (LTS) với tính năng DTO `record` bất biến.
* **Framework:** Spring Boot 4.1.2 (Spring Framework 7, Jakarta EE 11).
* **Tài liệu hóa API:** `springdoc-openapi-starter-webmvc-ui` 3.1.1 (OpenAPI 3.1).
* **Object Mapping:** MapStruct 1.6.3 (`componentModel = "spring"`).
* **Xử lý lỗi chuẩn RFC 9457:** `ProblemDetail` trả về `application/problem+json` với mã lỗi nghiệp vụ mở rộng.
* **Kiểm soát đồng thời (Optimistic Locking):** Trường `@Version` trên Entity `Order` kết hợp ETag và Header `If-Match`.

---

## 📋 5. DANH MỤC QUY TẮC NGHIỆP VỤ (BUSINESS RULES)

* **BR-01 (Server-Side Price Calculation):** Client không gửi giá hoặc tổng tiền. Server tự lấy giá snapshot tại thời điểm đặt hàng và tính toán.
* **BR-02 (Order Item Constraints):** Số lượng mỗi sản phẩm từ 1 đến 20; tối đa 50 mặt hàng/đơn; cấm tuyệt đối trùng `productId`.
* **BR-03 (Idempotency Control 24h):** Bắt buộc Header `Idempotency-Key` khi tạo đơn. Cùng key + cùng payload trả về kết quả đã lưu; cùng key + khác payload trả về HTTP `409 Conflict`.
* **BR-04 (Chính sách hủy đơn):** Khách hàng chỉ hủy khi đơn ở trạng thái `PENDING`. Quản trị viên hủy đơn `CONFIRMED` bắt buộc phải nhập `reason`.
* **BR-05 (Chính sách trả hàng 7 ngày):** Yêu cầu trả hàng (`REQUEST_RETURN`) chỉ hợp lệ trong vòng 7 ngày kể từ khi `DELIVERED`.
* **BR-06 (Optimistic Locking & ETag):** Bắt buộc Header `If-Match` khi gọi chuyển trạng thái; sai version trả về HTTP `412 Precondition Failed`.

---

## 🔄 6. MA TRẬN MÁY TRẠNG THÁI (7 STATES, 8 ACTIONS)

| Trạng thái Hiện tại | Hành động (Action) | Trạng thái Mới | Vai trò được Phép | Ràng buộc nghiệp vụ |
| :--- | :--- | :--- | :--- | :--- |
| `PENDING` | `CONFIRM` | `CONFIRMED` | ADMIN | Đã thanh toán hoặc xác nhận COD |
| `PENDING` | `CANCEL` | `CANCELLED` | CUSTOMER / ADMIN | Khách hủy trước khi đơn duyệt |
| `CONFIRMED` | `SHIP` | `SHIPPING` | ADMIN / PARTNER | Xuất kho cho đơn vị vận chuyển |
| `CONFIRMED` | `CANCEL` | `CANCELLED` | ADMIN | Bắt buộc phải có lý do (`reason`) |
| `SHIPPING` | `DELIVER` | `DELIVERED` | PARTNER | Giao hàng thành công (lưu `deliveredAt`) |
| `SHIPPING` | `FAIL_DELIVERY` | `CONFIRMED` | PARTNER | Giao thất bại, đưa về hàng chờ giao lại |
| `DELIVERED` | `REQUEST_RETURN` | `RETURN_REQUESTED` | CUSTOMER | Trong vòng 7 ngày sau khi giao |
| `RETURN_REQUESTED` | `ACCEPT_RETURN` | `RETURNED` | ADMIN | Nhận lại hàng và hoàn tiền |

---

## 📁 7. CẤU TRÚC THƯ MỤC TÀI LIỆU & SPEC YAMLS

```
docs/
├── 00_Project_Brief.md
├── 02_Architecture_Map.md
├── 04_API_Inventory.md
├── 05_Database_Schema.md
├── REST_API_Design_Guide.md
├── SRS_TechShop_OrderService.md
├── ADR_API_Versioning_Strategy.md    # ADR 1 trang giải trình chọn Versioning
├── api/
│   ├── openapi-public-v1.yaml         # Spec cho App Khách hàng v1 (Deprecated totalAmount)
│   ├── openapi-public-v2.yaml         # Spec cho App Khách hàng v2 (Money object)
│   ├── openapi-admin.yaml             # Spec cho Quản trị viên TechShop
│   └── openapi-partner.yaml           # Spec cho Đối tác giao hàng (Webhook)
└── 16_10_2026___7dfbb1c0-1d62-48a7-ad79-5a893d22a496.pdf
```
