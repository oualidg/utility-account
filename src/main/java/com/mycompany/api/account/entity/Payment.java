/*
 * Copyright (c) 2026 Oualid Gharach. All rights reserved.
 *
 * Aiming for production-grade standards through clean code and best practices
 * for educational and informational purposes.
 *
 * Created on: 2/6/2026 at 6:01 PM
 *
 * Feel free to use or contribute. Contact: oualid.gharach@gmail.com
 */
package com.mycompany.api.account.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.domain.Persistable;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Payment entity representing a payment transaction.
 * Receipt number (UUID v7) is used as primary key.
 *
 * <p>Uses @Getter/@Setter instead of @Data to avoid Lombok-generated equals/hashCode
 * on all fields, which is problematic with Hibernate proxies and lazy-loaded associations.
 * equals/hashCode are based on the business key (receiptNumber) only.</p>
 *
 * @author Oualid Gharach
 */
@Entity
@Table(name = "payments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Payment implements Persistable<String> {

    /**
     * Receipt number - UUID v7 format (36 characters).
     * Primary key for payment lookups.
     */
    @Id
    @Column(name = "receipt_number", nullable = false, length = 36)
    private String receiptNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_number", nullable = false)
    private Account account;

    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "provider_id", nullable = false)
    private PaymentProvider paymentProvider;

    /**
     * Payment reference for idempotency (64 characters max).
     */
    @Column(name = "payment_reference", nullable = false, length = 64)
    private String paymentReference;

    @Column(name = "payment_date", nullable = false, updatable = false)
    private Instant paymentDate;

    @Override
    public String getId() {
        return receiptNumber;
    }

    @Override
    public boolean isNew() {
        // Always INSERT for manually-assigned UUIDs (prevents unnecessary SELECT)
        return true;
    }

    @PrePersist
    protected void onCreate() {
        this.paymentDate = Instant.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Payment payment = (Payment) o;
        return receiptNumber != null && receiptNumber.equals(payment.receiptNumber);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}