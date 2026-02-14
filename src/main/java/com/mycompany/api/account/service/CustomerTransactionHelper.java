/*
 * Copyright (c) 2026 Oualid Gharach. All rights reserved.
 *
 * Aiming for production-grade standards through clean code and best practices
 * for educational and informational purposes.
 *
 * Created on: 2/13/2026 at 9:17 PM
 *
 * Feel free to use or contribute. Contact: oualid.gharach@gmail.com
 */
package com.mycompany.api.account.service;

import com.mycompany.api.account.dto.CreateCustomerRequest;
import com.mycompany.api.account.dto.CustomerDetailedResponse;
import com.mycompany.api.account.entity.Account;
import com.mycompany.api.account.entity.Customer;
import com.mycompany.api.account.exception.DuplicateResourceException;
import com.mycompany.api.account.mapper.CustomerMapper;
import com.mycompany.api.account.repository.CustomerRepository;
import com.mycompany.api.account.util.LuhnGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Transactional helper for customer creation.
 *
 * <p>Extracted into a separate bean so that {@link CustomerService#createCustomer}
 * can use @Retryable <em>outside</em> the transaction boundary. Each retry attempt
 * gets a fresh transaction and clean persistence context, preventing the "poisoned
 * EntityManager" problem that occurs when @Retryable and @Transactional are stacked
 * on the same proxy method.</p>
 *
 * @author Oualid Gharach
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class CustomerTransactionHelper {

    private final CustomerRepository customerRepository;
    private final CustomerMapper customerMapper;
    private final AccountService accountService;
    private final LuhnGenerator luhnGenerator;

    /**
     * Create a new customer with main account in a single transaction.
     *
     * <p>This method generates a Luhn customer ID and account number. If either
     * collides with an existing record, a DataIntegrityViolationException is thrown
     * at commit time, which the caller (CustomerService) retries with a fresh ID.</p>
     *
     * @param request the customer creation request
     * @return the created customer response
     * @throws DuplicateResourceException if email already exists (should not be retried)
     */
    @Transactional
    public CustomerDetailedResponse executeCreateCustomer(CreateCustomerRequest request) {
        log.info("Creating customer with email: {}", request.email());
        log.debug("Full request: {}", request);

        // 1. Check email availability across ALL customers (active and soft-deleted).
        // Uses a native query to bypass @SQLRestriction, ensuring we catch conflicts
        // with soft-deleted customers at the application level (clean 409)
        // instead of letting the DB unique constraint throw a raw 500.
        String normalizedEmail = customerMapper.normalizeEmail(request.email());
        if (customerRepository.existsByEmailIncludingInactive(normalizedEmail)) {
            throw new DuplicateResourceException("Customer with email " + normalizedEmail + " already exists");
        }

        // 2. Map and generate a fresh ID for this attempt
        Customer customer = customerMapper.toEntity(request);
        customer.setCustomerId(luhnGenerator.generateCustomerId());

        // 3. Save Customer — flush immediately so a PK collision throws
        // DataIntegrityViolationException here, not later during account creation.
        Customer savedCustomer = customerRepository.saveAndFlush(customer);

        // 4. Create main account
        Account mainAccount = accountService.createMainAccount(savedCustomer);

        log.info("Customer created successfully with ID: {}", savedCustomer.getCustomerId());

        return customerMapper.toDetailedResponse(savedCustomer, List.of(mainAccount));
    }
}