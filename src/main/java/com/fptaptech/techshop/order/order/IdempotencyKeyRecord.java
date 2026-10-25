package com.fptaptech.techshop.order.order;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "idempotency_keys")
@Getter
@Setter
@NoArgsConstructor
public class IdempotencyKeyRecord {

    @Id
    @Column(name = "key_value", length = 100)
    private String keyValue;

    @Column(name = "request_hash", nullable = false, length = 64)
    private String requestHash;

    @Lob
    @Column(name = "response_body", nullable = false, columnDefinition = "TEXT")
    private String responseBody;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    public IdempotencyKeyRecord(String keyValue, String requestHash, String responseBody) {
        this.keyValue = keyValue;
        this.requestHash = requestHash;
        this.responseBody = responseBody;
    }
}
