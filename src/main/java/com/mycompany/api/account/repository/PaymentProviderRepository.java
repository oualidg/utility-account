/*
 * Copyright (c) 2026 Oualid Gharach. All rights reserved.
 *
 * Aiming for production-grade standards through clean code and best practices
 * for educational and informational purposes.
 *
 * Created on: 2/17/2026 at 9:25 PM
 *
 * Feel free to use or contribute. Contact: oualid.gharach@gmail.com
 */
package com.mycompany.api.account.repository;

import com.mycompany.api.account.entity.PaymentProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for PaymentProvider entity.
 *
 * @author Oualid Gharach
 */
@Repository
public interface PaymentProviderRepository extends JpaRepository<PaymentProvider, Long> {

    /**
     * Find provider by unique code (e.g., "MPESA", "MTN").
     *
     * @param code provider code
     * @return optional containing provider if found
     */
    Optional<PaymentProvider> findByCode(String code);

    /**
     * Find all active providers.
     *
     * @return list of active providers
     */
    List<PaymentProvider> findByActiveTrue();
}