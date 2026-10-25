# API INVENTORY: TECHSHOP ORDER SERVICE

| HTTP Method | Endpoint Path | Spec Group | Request Body / Params | Response Body | Status Codes |
| :--- | :--- | :--- | :--- | :--- | :--- |
| `POST` | `/api/orders` | public-v1, public-v2 | `OrderRequest` (Record), Header: `Idempotency-Key` | `OrderResponse` (Record), Header: `Location` | `201`, `400`, `409` |
| `GET` | `/api/orders` | public-v1, public-v2 | Query: `status`, `from`, `to`, `minTotal`, `page`, `size`, `sort` | `PageResponse<OrderResponse>` | `200`, `401` |
| `GET` | `/api/orders/{code}` | public-v1, public-v2 | Path: `code` | `OrderResponseV1` / `OrderResponseV2` | `200`, `404` |
| `POST` | `/api/orders/{code}/transitions` | admin, public-v1, public-v2 | Path: `code`, Body: `{action, reason}`, Header: `If-Match` | `OrderResponse` | `200`, `400`, `409`, `412` |
| `POST` | `/api/orders/{code}/invoice` | admin | Path: `code`, Multipart File: `file` (PDF <= 5MB) | `InvoiceResponse` | `200`, `400`, `404` |
| `GET` | `/api/orders/{code}/invoice` | public-v1, public-v2 | Path: `code` | Binary `application/pdf` | `200`, `404` |
| `POST` | `/api/webhooks/shipping` | partner | Header: `X-Partner-Key`, Body: Shipping Status Payload | `WebhookResponse` | `200`, `401`, `409` |
| `GET` | `/api/admin/reports/daily-revenue` | admin | Query: `from`, `to` | `DailyRevenueReport` | `200`, `401`, `403` |
