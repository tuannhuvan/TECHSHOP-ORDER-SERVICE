# ĐẶC TẢ YÊU CẦU PHẦN MỀM (SRS) - TECHSHOP ORDER SERVICE
**Dự án:** Dịch vụ Quản lý Đơn hàng (Order Service) cho TechShop  
**Phiên bản:** 1.0.0  
**Tác giả:** Senior Business Analyst (BA Cấp cao)  
**Tiêu chuẩn áp dụng:** REST API Specification (Phil Sturgeon), Spring Boot 4.1 + OpenAPI 3.1, RFC 9457  

---

## 1. TỔNG QUAN HỆ THỐNG
Dịch vụ **Order Service** cho **TechShop** là một microservice độc lập chịu trách nhiệm xử lý toàn bộ vòng đời đơn hàng, thanh toán, nhật ký chuyển trạng thái, và báo cáo doanh thu. Dịch vụ này tái sử dụng module sản phẩm (`product`) và phục vụ 3 nhóm đối tượng sử dụng (clients) chính:
1. **Khách hàng (Customer App):** Đặt hàng, theo dõi đơn hàng, hủy đơn, yêu cầu trả hàng.
2. **Quản trị viên (Admin Dashboard):** Duyệt đơn, chuyển trạng thái đơn, upload hóa đơn PDF, xem báo cáo doanh thu.
3. **Đối tác vận chuyển (Shipping Partner):** Cập nhật trạng thái giao hàng thông qua Webhook bảo mật.

---

## 2. TÁC NHÂN HỆ THỐNG (ACTORS)
| Actor | Phương thức Xác thực | Quyền hạn / Phạm vi tác động |
| :--- | :--- | :--- |
| **CUSTOMER** | Bearer JWT (`CUSTOMER`) | - Tạo đơn hàng (yêu cầu `Idempotency-Key`) <br>- Xem danh sách & chi tiết đơn hàng của chính mình <br>- Thực hiện action `CANCEL` (khi PENDING) & `REQUEST_RETURN` (trong 7 ngày sau DELIVERED) |
| **ADMIN** | Bearer JWT (`ADMIN`) | - Xem tất cả đơn hàng, lọc/tìm kiếm nâng cao <br>- Thực hiện mọi action chuyển trạng thái đơn hàng <br>- Upload hóa đơn PDF (<= 5MB) <br>- Xem báo cáo doanh thu ngày |
| **PARTNER** | API Key Header (`X-Partner-Key`) | - Gọi Webhook cập nhật trạng thái vận chuyển (`SHIPPING`, `DELIVERED`, `FAIL_DELIVERY`) |

---

## 3. MÔ HÌNH DỮ LIỆU NGHIỆP VỤ (BUSINESS DATA MODEL)

### 3.1 Các Thực thể Chính (Entities)
1. **Customer:** `id`, `fullName`, `email` (Unique), `phone`.
2. **Order:** `id`, `code` (mã đơn định dạng `ORD-2026-XXXXXX`), `customerId`, `status`, `shippingAddress`, `totalAmount`, `currency`, `version` (Optimistic locking & ETag), `createdAt`.
3. **OrderItem:** `productId`, `productName`, `unitPrice`, `quantity`, `lineTotal`.
   - *Lưu ý:* `productName` và `unitPrice` là **bản chụp (snapshot)** tại thời điểm đặt hàng để tránh biến động giá sau này.
4. **Payment (Đa hình / Polymorphic Payment):** Phân loại qua `method` (`COD` | `CARD` | `BANK_TRANSFER`):
   - `CARD`: `provider`, `last4`.
   - `BANK_TRANSFER`: `bankCode`, `transferRef`.
   - `COD`: Không có trường riêng.
5. **OrderEvent:** `id`, `orderId`, `fromStatus`, `toStatus`, `action`, `actor`, `reason`, `at` (Nhật ký kiểm toán chuyển trạng thái).

---

## 4. MÁY TRẠNG THÁI ĐƠN HÀNG (ORDER STATE MACHINE)

### 4.1 Các Trạng thái (7 States) & Hành động (8 Actions)
- **Trạng thái:** `PENDING`, `CONFIRMED`, `SHIPPING`, `DELIVERED`, `CANCELLED`, `RETURN_REQUESTED`, `RETURNED`.
- **Trạng thái kết thúc (Terminal states):** `CANCELLED`, `RETURNED`.

### 4.2 Ma trận Chuyển Trạng thái (State Transition Matrix)
| Trạng thái Hiện tại | Hành động (Action) | Trạng thái Mới | Vai trò được Phép (Actor) | Ràng buộc & Điều kiện |
| :--- | :--- | :--- | :--- | :--- |
| `PENDING` | `CONFIRM` | `CONFIRMED` | ADMIN | Khách hàng đã thanh toán hoặc xác nhận COD |
| `PENDING` | `CANCEL` | `CANCELLED` | CUSTOMER / ADMIN | Khách chỉ hủy được khi đơn còn PENDING |
| `CONFIRMED` | `SHIP` | `SHIPPING` | ADMIN / PARTNER | Đã đóng gói, chuyển đối tác giao hàng |
| `CONFIRMED` | `CANCEL` | `CANCELLED` | ADMIN | Bắt buộc phải cung cấp lý do (`reason`) |
| `SHIPPING` | `DELIVER` | `DELIVERED` | PARTNER | Giao hàng thành công |
| `SHIPPING` | `FAIL_DELIVERY` | `CONFIRMED` | PARTNER | Giao thất bại, đưa về CONFIRMED để giao lại |
| `DELIVERED` | `REQUEST_RETURN` | `RETURN_REQUESTED` | CUSTOMER | Chỉ hợp lệ trong vòng 7 ngày kể từ khi DELIVERED |
| `RETURN_REQUESTED` | `ACCEPT_RETURN` | `RETURNED` | ADMIN | Duyệt nhận lại hàng và hoàn tiền |

> **Xử lý vi phạm:** Chuyển trạng thái không hợp lệ trả về HTTP `409 Conflict` với mã lỗi `ORDER_INVALID_TRANSITION`.

---

## 5. QUY TẮC NGHIỆP VỤ (BUSINESS RULES CATALOG)

- **BR-01 (Server-Side Price Calculation):** Client **không được phép** gửi tổng tiền (`totalAmount`) hoặc giá sản phẩm. Backend tự lấy giá từ module Product tại thời điểm đặt hàng và tính toán `totalAmount`.
- **BR-02 (Order Item Constraints):**
  - Số lượng từng mặt hàng (`quantity`): Từ 1 đến 20.
  - Số dòng sản phẩm tối đa trong 1 đơn hàng: Tối đa 50 dòng (`OrderItem`).
  - Không được trùng `productId` trong cùng một đơn hàng.
- **BR-03 (Idempotency Control):**
  - Đòi hỏi Header `Idempotency-Key` bắt buộc khi tạo đơn hàng (`POST /api/orders`).
  - Cùng `Idempotency-Key` + cùng request body trong vòng 24 giờ -> Trả lại đúng kết quả lần tạo đầu tiên.
  - Cùng `Idempotency-Key` + khác request body -> Trả về lỗi `409 Conflict` với mã lỗi `IDEMPOTENCY_CONFLICT`.
- **BR-04 (Cancellation Policy):**
  - CUSTOMER chỉ được hủy khi đơn ở trạng thái `PENDING`.
  - ADMIN hủy đơn ở trạng thái `CONFIRMED` bắt buộc phải điền trường `reason`.
- **BR-05 (Return Policy):** Yêu cầu trả hàng (`REQUEST_RETURN`) chỉ hợp lệ trong vòng 7 ngày tính từ thời điểm chuyển sang `DELIVERED`.
- **BR-06 (Optimistic Locking & ETag):**
  - Mọi yêu cầu chuyển trạng thái (`POST /api/orders/{code}/transitions`) bắt buộc gửi kèm Header `If-Match` mang giá trị `version` hiện tại của đơn hàng.
  - Nếu `If-Match` lệch với `version` trong DB -> Trả về HTTP `412 Precondition Failed`.

---

## 6. DANH SÁCH REST API ENDPOINTS
| Method | Endpoint Path | Nhóm Spec | Auth / Role | Mô tả Tóm tắt |
| :--- | :--- | :--- | :--- | :--- |
| `POST` | `/api/orders` | public-v1 / public-v2 | Bearer JWT (CUSTOMER) | Tạo đơn hàng mới. Bắt buộc header `Idempotency-Key`. |
| `GET` | `/api/orders` | public-v1 / public-v2 | Bearer JWT | Phân trang, lọc (`status`, `from`, `to`, `minTotal`), sort. Customer chỉ xem đơn của mình. |
| `GET` | `/api/orders/{code}` | public-v1 (deprecated) / public-v2 | Bearer JWT | Xem chi tiết. v1 trả `totalAmount` (số), v2 trả `total: {value, currency}`. |
| `POST` | `/api/orders/{code}/transitions` | admin & public-v1 / public-v2 | Bearer JWT | Chuyển trạng thái đơn. Admin làm mọi action; Customer làm `CANCEL`, `REQUEST_RETURN`. Yêu cầu `If-Match`. |
| `POST` | `/api/orders/{code}/invoice` | admin | Bearer JWT (ADMIN) | Upload hóa đơn PDF (multipart/form-data, <= 5 MB). |
| `GET` | `/api/orders/{code}/invoice` | public-v1 / public-v2 | Bearer JWT | Tải file PDF hóa đơn (`application/pdf`). |
| `POST` | `/api/webhooks/shipping` | partner | Header `X-Partner-Key` | Đối tác vận chuyển báo `SHIPPING`, `DELIVERED`, `FAIL_DELIVERY`. |
| `GET` | `/api/admin/reports/daily-revenue` | admin | Bearer JWT (ADMIN) | Báo cáo doanh thu theo ngày, lọc `from` / `to`. |
