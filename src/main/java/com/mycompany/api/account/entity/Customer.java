/*
 * Copyright (c) 2026 Oualid Gharach. All rights reserved.
 *
 * Aiming for production-grade standards through clean code and best practices
 * for educational and informational purposes.
 *
 * Created on: 2/3/2026 at 9:11 PM
 *
 * Feel free to use or contribute. Contact: oualid.gharach@gmail.com
 */
package com.mycompany.api.account.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Customer entity representing the customers table in the database.
 * Uses JPA lifecycle callbacks (@PrePersist, @PreUpdate) for automatic timestamp management.
 *
 * @author Oualid Gharach
 */
@Entity
@Table(name = "customers")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Customer {

    @Id
    @Column(name = "customer_id")
    private Long customerId;

    @Column(name = "first_name", nullable = false, length = 50)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 50)
    private String lastName;

    @Column(name = "email", unique = true, nullable = false, length = 100)
    private String email;

    @Column(name = "mobile_number", nullable = false, length = 15)
    private String mobileNumber;

    /**
     * Automatically set when entity is first created.
     * Never updated after initial creation.
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /**
     * Automatically set when entity is first created.
     */
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /**
     * JPA callback - executed before entity is persisted to database.
     * Sets both createdAt and updatedAt on first save.
     */
    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

}