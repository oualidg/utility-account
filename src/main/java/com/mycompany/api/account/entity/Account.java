/*
 * Copyright (c) 2026 Oualid Gharach. All rights reserved.
 *
 * Aiming for production-grade standards through clean code and best practices
 * for educational and informational purposes.
 *
 * Created on: 2/6/2026 at 6:08 PM
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
 * Account entity representing a utility account.
 * Each account is identified by a 10-digit Luhn checksum account number.
 * One customer can have multiple accounts (main account + additional accounts).
 *
 * <p>Implements {@link Persistable} to force INSERT on save — without this, Hibernate
 * sees a non-null manually-assigned ID and does a merge instead of insert.</p>
 *
 * <p>Uses @Getter/@Setter instead of @Data to avoid Lombok-generated equals/hashCode
 * on all fields, which is problematic with Hibernate proxies and lazy-loaded associations.
 * equals/hashCode are based on the business key (accountNumber) only.</p>
 *
 * @author Oualid Gharach
 */
@Entity
@Table(name = "accounts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Account implements Persistable<Long> {

    /**
     * Account number - 10-digit Luhn checksum number.
     */
    @Id
    @Column(name = "account_number", nullable = false)
    private Long accountNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(name = "balance", nullable = false, precision = 15, scale = 2)
    private BigDecimal balance;

    /**
     * Flag indicating if this is the customer's main account.
     * Primitive boolean for NOT NULL column — avoids unnecessary boxing.
     */
    @Column(name = "is_main_account", nullable = false)
    private boolean mainAccount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    /**
     * JPA callback - executed before entity is updated in database.
     * Automatically maintains updatedAt timestamp.
     */
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    @Override
    public Long getId() {
        return accountNumber;
    }

    @Override
    public boolean isNew() {
        return createdAt == null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Account account = (Account) o;
        return accountNumber != null && accountNumber.equals(account.accountNumber);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}