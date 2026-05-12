/*
 * Copyright (c) 2026 Oualid Gharach. All rights reserved.
 *
 * Aiming for production-grade standards through clean code and best practices
 * for educational and informational purposes.
 *
 * Created on: 5/11/2026 at 8:36 PM
 *
 * Feel free to use or contribute. Contact: oualid.gharach@gmail.com
 */
package com.mycompany.api.account.service;

import com.mycompany.api.account.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for pre-payment validation checks used by external payment gateways.
 *
 * <p>Exposes account and customer existence checks without exposing balance or
 * sensitive billing data. Used by external payment gateway integrations during
 * pre-payment validation flows. </p>
 *
 * <p>Responses are intentionally status-only — no business data is returned.</p>
 *
 * <p>Both methods are read-only and do not modify any payment or account state.</p>
 *
 * @author Oualid Gharach
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentValidationService {

    private final AccountRepository accountRepository;

    /**
     * Validate that an account exists and is eligible to receive a payment.
     *
     * <p>Returns {@code true} if an active account with the given account number
     * exists. The existing {@code findActiveByAccountNumber} query enforces that
     * the owning customer is active via {@code JOIN FETCH ... WHERE c.active = true}.</p>
     *
     * <p>Does not check balance or perform posting-time business rules beyond active
     * account/customer eligibility — those constraints are enforced by the deposit
     * endpoint at posting time.</p>
     *
     * @param accountNumber 10-digit Luhn account number
     * @return {@code true} if the account is valid and active, {@code false} otherwise
     */
    @Transactional(readOnly = true)
    public boolean isAccountValid(Long accountNumber) {
        return accountRepository.findActiveByAccountNumber(accountNumber).isPresent();
    }

    /**
     * Validate that a customer exists and has a main account eligible to receive a payment.
     *
     * <p>Returns {@code true} if an active customer with the given customer ID has a
     * main account. {@code @SQLRestriction("active = true")} on the {@code Customer}
     * entity ensures inactive customers return no result without a separate query.</p>
     *
     * <p>Does not check balance or perform posting-time business rules beyond active
     * account/customer eligibility — those constraints are enforced by the deposit
     * endpoint at posting time.</p>
     *
     * @param customerId 8-digit Luhn customer ID
     * @return {@code true} if the customer is active and has a main account, {@code false} otherwise
     */
    @Transactional(readOnly = true)
    public boolean isCustomerValid(Long customerId) {
        return accountRepository.findByCustomer_CustomerIdAndMainAccountTrue(customerId).isPresent();
    }
}