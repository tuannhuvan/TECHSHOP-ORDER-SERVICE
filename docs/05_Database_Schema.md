# DATABASE SCHEMA: TECHSHOP ORDER SERVICE (MYSQL 8.4)

---

## 1. SƠ ĐỒ CÁC BẢNG (TABLE SCHEMAS)

### 1.1 Bảng `customers`
- `id` BIGINT AUTO_INCREMENT PRIMARY KEY
- `full_name` VARCHAR(120) NOT NULL
- `email` VARCHAR(120) NOT NULL UNIQUE
- `phone` VARCHAR(20) NOT NULL

### 1.2 Bảng `orders`
- `id` BIGINT AUTO_INCREMENT PRIMARY KEY
- `code` VARCHAR(30) NOT NULL UNIQUE (Format: `ORD-2026-XXXXXX`)
- `customer_id` BIGINT NOT NULL (FK -> `customers.id`)
- `status` VARCHAR(30) NOT NULL (`PENDING`, `CONFIRMED`, `SHIPPING`, `DELIVERED`, `CANCELLED`, `RETURN_REQUESTED`, `RETURNED`)
- `shipping_address` VARCHAR(500) NOT NULL
- `total_amount` DECIMAL(12, 2) NOT NULL
- `currency` VARCHAR(3) NOT NULL DEFAULT 'VND'
- `payment_method` VARCHAR(20) NOT NULL (`COD`, `CARD`, `BANK_TRANSFER`)
- `card_provider` VARCHAR(50) NULL
- `card_last4` VARCHAR(4) NULL
- `bank_code` VARCHAR(20) NULL
- `transfer_ref` VARCHAR(100) NULL
- `version` BIGINT NOT NULL DEFAULT 0 (Optimistic Locking & ETag)
- `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
- `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP

### 1.3 Bảng `order_items`
- `id` BIGINT AUTO_INCREMENT PRIMARY KEY
- `order_id` BIGINT NOT NULL (FK -> `orders.id` ON DELETE CASCADE)
- `product_id` BIGINT NOT NULL
- `product_name` VARCHAR(120) NOT NULL (Snapshot)
- `unit_price` DECIMAL(12, 2) NOT NULL (Snapshot)
- `quantity` INT NOT NULL CHECK (quantity BETWEEN 1 AND 20)
- `line_total` DECIMAL(12, 2) NOT NULL

### 1.4 Bảng `order_events`
- `id` BIGINT AUTO_INCREMENT PRIMARY KEY
- `order_id` BIGINT NOT NULL (FK -> `orders.id`)
- `from_status` VARCHAR(30) NOT NULL
- `to_status` VARCHAR(30) NOT NULL
- `action` VARCHAR(30) NOT NULL
- `actor` VARCHAR(120) NOT NULL
- `reason` VARCHAR(500) NULL
- `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP

### 1.5 Bảng `idempotency_keys`
- `key_value` VARCHAR(100) PRIMARY KEY
- `request_hash` VARCHAR(64) NOT NULL
- `response_body` TEXT NOT NULL
- `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
