/*
 * Copyright (c) 2026 Oualid Gharach. All rights reserved.
 *
 * Aiming for production-grade standards through clean code and best practices
 * for educational and informational purposes.
 *
 * Created on: 2/6/2026 at 9:38 PM
 *
 * Feel free to use or contribute. Contact: oualid.gharach@gmail.com
 */
package com.mycompany.api.account.repository;

import com.mycompany.api.account.entity.Payment;
import com.mycompany.api.account.entity.PaymentProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository interface for Payment entity.
 * Provides database access methods for payment transaction management.
 *
 * @author Oualid Gharach
 */
@Repository
public interface PaymentRepository extends JpaRepository<Payment, String> {

    /**
     * Find payment by provider and reference with eager fetch of provider and account.
     * Used for confirmation queries where the full payment details are needed outside
     * a transaction boundary.
     *
     * @param provider payment provider entity
     * @param reference payment reference from provider
     * @return optional containing fully loaded payment
     */
    @Query("SELECT p FROM Payment p " +
            "JOIN FETCH p.paymentProvider " +
            "JOIN FETCH p.account " +
            "WHERE p.paymentProvider = :provider AND p.paymentReference = :reference")
    Optional<Payment> findByProviderAndReferenceWithDetails(
            PaymentProvider provider,
            String reference
    );

}