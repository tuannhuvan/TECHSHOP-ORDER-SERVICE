# HƯỚNG DẪN THIẾT KẾ REST API THEO CHUẨN ĐẶC TẢ
**Định hướng thiết kế:** Áp dụng nguyên tắc từ "Build APIs You Won't Hate" (Phil Sturgeon) & Cấu hình Spring Boot 4.1 + springdoc-openapi 3.1.

---

## 1. NGUYÊN TẮC THIẾT KẾ ENDPOINT & RESOURCE (PHIL STURGEON)
1. **Dùng Danh từ Số nhiều (Plural Nouns):**
   - Đặt URL dạng `/api/orders`, `/api/products` thay vì danh từ số ít hay động từ (`/createOrder`, `/getOrder`).
   - Không chứa hành động (verbs) trên URL path; hành động được thể hiện qua HTTP Verb (`POST`, `GET`, `PUT`, `DELETE`).
2. **Sử dụng Định danh An toàn:**
   - Mã đơn hàng dùng chuỗi định dạng business `ORD-2026-000123` thay vì Auto-increment ID công khai nhằm tránh bị lừa vét dữ liệu (scraping) hoặc lộ quy mô kinh doanh.
3. **Phân biệt POST vs PUT vs PATCH:**
   - `POST`: Tạo mới tài nguyên không idempotent hoặc thực thi action chuyển trạng thái.
   - `PUT`: Thay thế toàn bộ tài nguyên (Idempotent).
   - `PATCH`: Cập nhật một phần trường dữ liệu.

---

## 2. CHUẨN XỬ LÝ LỖI MỞ RỘNG (RFC 9457 - PROBLEM DETAIL)
Tất cả các lỗi trả về từ API đều sử dụng định dạng `application/problem+json` chuẩn hóa với cấu trúc schema `ApiProblem`:

```json
{
  "type": "https://example.com/errors/validation",
  "title": "Validation failed",
  "status": 400,
  "code": "INVALID_INPUT",
  "detail": "Dữ liệu yêu cầu không hợp lệ.",
  "instance": "/api/orders",
  "timestamp": "2026-10-23T10:15:30Z",
  "errors": [
    {
      "field": "items[0].quantity",
      "message": "Số lượng phải từ 1 đến 20"
    }
  ]
}
```

### Các Mã HTTP Status Code Chuẩn
- `200 OK`: Truy vấn hoặc cập nhật thành công.
- `201 Created`: Tạo đơn/tài nguyên thành công (trả kèm Header `Location`).
- `204 No Content`: Xóa thành công, không có body.
- `400 Bad Request`: Dữ liệu không hợp lệ / Vi phạm validation.
- `401 Unauthorized`: Thiếu hoặc sai JWT token / X-Partner-Key.
- `403 Forbidden`: Quyền hạn không đủ tác động.
- `404 Not Found`: Không tìm thấy đơn hàng / tài nguyên.
- `409 Conflict`: Xung đột idempotency (`IDEMPOTENCY_CONFLICT`) hoặc máy trạng thái (`ORDER_INVALID_TRANSITION`).
- `412 Precondition Failed`: Lệch `version` đơn hàng khi kiểm tra `If-Match`.

---

## 3. TÁCH NHÓM API SPEC & SECURITY SCHEMES (SPRINGDOC OPENAPI 3.1)

### 3.1 Tách 4 Nhóm Spec OpenAPI (Swagger UI Dropdown)
1. `public-v1`: API Khách hàng v1 (Đã deprecated `totalAmount` dạng số).
2. `public-v2`: API Khách hàng v2 (`total: {value, currency}`).
3. `admin`: API Quản trị viên TechShop.
4. `partner`: API Dành cho đối tác vận chuyển.

### 3.2 Security Schemes
- `bearerAuth`: HTTP Bearer JWT token dành cho CUSTOMER và ADMIN.
- `partnerKey`: API Key truyền trong Header `X-Partner-Key` dành cho PARTNER.

---

## 4. CHUẨN PHÂN TRANG VÀ LỌC DỮ LIỆU (PAGINATION ENVELOPE)
Dữ liệu trả về phân trang tuân thủ vỏ bọc ổn định (`data` / `content` + `page`), không lộ trực tiếp cấu trúc `PageImpl` của Spring Data JPA:

```json
{
  "content": [
    {
      "id": 1,
      "code": "ORD-2026-000123",
      "status": "CONFIRMED",
      "total": { "value": 1590000, "currency": "VND" },
      "createdAt": "2026-10-23T08:00:00Z"
    }
  ],
  "page": {
    "size": 20,
    "number": 0,
    "totalElements": 1,
    "totalPages": 1
  }
}
```
