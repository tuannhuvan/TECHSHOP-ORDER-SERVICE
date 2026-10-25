# ADR 001: LỰA CHỌN CHIẾN LƯỢC VERSIONING API (URI PATH VS HEADER CONTENT NEGOTIATION)

**Trạng thái:** ACCEPTED (Đã duyệt)  
**Tác giả:** Senior Solution Architect & Senior BA  
**Dự án:** TechShop Order Service  
**Ngày quyết định:** 25/10/2026  

---

## 1. BỐI CẢNH & VẤN ĐỀ (CONTEXT & PROBLEM STATEMENT)

Trong quá trình phát triển hệ sinh thái thương mại điện tử TechShop, dịch vụ **Order Service** cần nâng cấp cấu trúc dữ liệu trả về của đơn hàng giữa hai thế hệ:
- **Phiên bản v1 (Legacy):** Trường tổng tiền trả về dạng số thực đơn thuần (`totalAmount: 1590000.00`), không hỗ trợ đa tiền tệ, gây khó khăn khi mở rộng thị trường quốc tế.
- **Phiên bản v2 (Target):** Trường tổng tiền được chuẩn hóa thành đối tượng tiền tệ đa quốc gia (`total: { "value": 1590000.00, "currency": "VND" }`).

Đây là một thay đổi phá vỡ tương thích ngược (**Breaking Change**). Hệ thống cần lựa chọn phương pháp quản lý phiên bản (API Versioning Strategy) tối ưu để phục vụ song song: ứng dụng Mobile App cũ (v1), Dashboard Web mới (v2), các đối tác tích hợp bên ngoài và công cụ tài liệu hóa OpenAPI 3.1.

---

## 2. CÁC PHƯƠNG ÁN ĐƯỢC XEM XÉT (CONSIDERED OPTIONS)

### Phương án A: Header Versioning / Content Negotiation
- **Cơ chế:** Giữ nguyên URI (`/api/orders/{code}`), truyền phiên bản qua Header:
  - Hoặc qua Custom Header: `X-API-Version: 2`
  - Hoặc qua Media Type Negotiation: `Accept: application/vnd.techshop.v2+json`
- **Ưu điểm:** URI đại diện thuần túy cho tài nguyên theo lý thuyết RESTful nghiêm ngặt; không nhân bản URL path.
- **Nhược điểm:**
  - Cực kỳ phức tạp cho tầng Caching (CDN, Reverse Proxy, Browser) do phải cấu hình `Vary: Accept, X-API-Version`.
  - Khó kiểm thử trực tiếp trên thanh địa chỉ trình duyệt hay công cụ Swagger UI chuẩn.
  - Các bộ sinh mã client tự động (`openapi-generator`) gặp khó khăn lớn khi một URL có nhiều response schema phụ thuộc header.

### Phương án B: URI Path Versioning (Được lựa chọn)
- **Cơ chế:** Đưa định danh phiên bản trực tiếp vào đường dẫn:
  - Endpoint cũ: `/api/v1/orders/{code}` (đánh dấu `@Deprecated`)
  - Endpoint chuẩn mới: `/api/orders/{code}` (mặc định v2) hoặc `/api/v2/orders/{code}`.
- **Ưu điểm:**
  - **Minh bạch & Trực quan (Phil Sturgeon - "Build APIs You Won't Hate"):** Mỗi URL gắn liền với một định dạng dữ liệu cố định, không phụ thuộc vào trạng thái ẩn trong HTTP Header.
  - **Tương thích hoàn hảo với Caching & CDN:** Các proxy trung gian dễ dàng cache độc lập theo URL mà không lo cache poisoning giữa v1 và v2.
  - **Tách bạch tài liệu OpenAPI 3.1:** springdoc-openapi dễ dàng phân tách thành 2 spec group độc lập (`public-v1` và `public-v2`) trên Swagger UI dropdown, giúp đối tác tra cứu không bị nhầm lẫn schema.
  - **Tạo mã SDK tự động (TypeScript / Mobile SDK):** `openapi-generator` sinh ra các hàm và kiểu dữ liệu rõ ràng (`OrderResponseV1` vs `OrderResponseV2`), loại bỏ lỗi parse JSON runtime.
- **Nhược điểm:** Tăng số lượng route trên Controller và tài liệu spec.

---

## 3. QUYẾT ĐỊNH (DECISION OUTCOME)

**TechShop quyết định lựa chọn Phương án B (URI Path Versioning) làm chiến lược cốt lõi:**
1. Giữ nguyên route v1 `/api/v1/orders/{code}` dành riêng cho các ứng dụng client cũ, đánh dấu `deprecated: true` trong đặc tả OpenAPI và bổ sung thời hạn ngừng hỗ trợ (**Sunset Header / Timeline**).
2. Đường dẫn chuẩn `/api/orders/{code}` phục vụ spec v2 hiện đại với cấu trúc tiền tệ `Money`.
3. Về mặt kiến trúc mã nguồn Spring Boot 4.1:
   - Tách hợp đồng thành Interface [`OrderApi`](file:///d:/06.Works/FPT-Aptech/Springboot/TechShop_OrderService/demoTechShopOrderService/src/main/java/com/fptaptech/techshop/order/order/OrderApi.java) chứa khai báo rõ ràng cả hai method: `getOrderByCode` (v2) và `getOrderByCodeV1` (v1).
   - Sử dụng MapStruct [`OrderMapper`](file:///d:/06.Works/FPT-Aptech/Springboot/TechShop_OrderService/demoTechShopOrderService/src/main/java/com/fptaptech/techshop/order/order/OrderMapper.java) để chuyển đổi từ cùng một JPA Entity `Order` sang `OrderResponseV1` hoặc `OrderResponseV2`, bảo đảm logic nghiệp vụ không bị phân mảnh.

---

## 4. HỆ QUẢ VÀ KẾ HOẠCH DÀI HẠN (CONSEQUENCES & SUNSET PLAN)

* **Tích cực:** Quá trình chuyển đổi an toàn 100%, không làm gián đoạn người dùng app v1, giao diện Swagger UI phân nhóm rõ ràng, client TypeScript biên dịch không lỗi.
* **Kế hoạch dọn dẹp (Sunset):** Sau 12 tháng kể từ ngày công bố v2, hệ thống sẽ trả về mã cảnh báo `Sunset: Sun, 25 Oct 2027 00:00:00 GMT` trước khi chính thức decommission toàn bộ endpoint `/api/v1/*`.
