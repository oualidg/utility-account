/*
 * Copyright (c) 2026 Oualid Gharach. All rights reserved.
 *
 * Aiming for production-grade standards through clean code and best practices
 * for educational and informational purposes.
 *
 * Created on: 2/17/2026 at 9:22 PM
 *
 * Feel free to use or contribute. Contact: oualid.gharach@gmail.com
 */
package com.mycompany.api.account.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Payment provider entity representing an external payment service (e.g., M-Pesa, MTN).
 *
 * <p>Replaces the former PaymentProvider enum to allow dynamic provider management,
 * API key authentication, and runtime activation/deactivation.</p>
 *
 * <p>Uses @Getter/@Setter instead of @Data to avoid Lombok-generated equals/hashCode
 * on all fields, which is problematic with Hibernate proxies.</p>
 *
 * @author Oualid Gharach
 */
@Entity
@Table(name = "payment_providers")
@Getter
@Setter
@NoArgsConstructor
public class PaymentProvider {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /**
     * Unique provider code (e.g., "MPESA", "MTN").
     * Used as the logical identifier in API requests and logs.
     */
    @Column(name = "code", nullable = false, unique = true, length = 50)
    private String code;

    /**
     * Human-readable provider name.
     */
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    /** SHA-256 hash of the raw API key. SHA-256 is appropriate here because keys
     *  are long random UUIDs (122 bits entropy) — brute force is computationally infeasible.
     */
    @Column(name = "api_key_hash", nullable = false, length = 255)
    private String apiKeyHash;

    /**
     * First 8 characters of the API key for identification purposes.
     * Allows support to identify a key without exposing it.
     */
    @Column(name = "api_key_prefix", nullable = false, length = 8)
    private String apiKeyPrefix;

    /**
     * Whether this provider is currently active and allowed to process payments.
     */
    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PaymentProvider that = (PaymentProvider) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "PaymentProvider{code='" + code + "'}";
    }
}