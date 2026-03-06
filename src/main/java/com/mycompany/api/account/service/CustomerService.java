/*
 * Copyright (c) 2026 Oualid Gharach. All rights reserved.
 *
 * Aiming for production-grade standards through clean code and best practices
 * for educational and informational purposes.
 *
 * Created on: 2/3/2026 at 9:58 PM
 *
 * Feel free to use or contribute. Contact: oualid.gharach@gmail.com
 */
package com.mycompany.api.account.service;

import com.mycompany.api.account.dto.CreateCustomerRequest;
import com.mycompany.api.account.dto.CustomerDetailedResponse;
import com.mycompany.api.account.dto.CustomerSummaryResponse;
import com.mycompany.api.account.dto.UpdateCustomerRequest;
import com.mycompany.api.account.entity.Customer;
import com.mycompany.api.account.exception.DuplicateResourceException;
import com.mycompany.api.account.exception.ResourceNotFoundException;
import com.mycompany.api.account.mapper.CustomerMapper;
import com.mycompany.api.account.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Service layer for Customer operations.
 * Contains business logic for creating, updating, retrieving, and deleting customers.
 *
 * @author Oualid Gharach
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final CustomerMapper customerMapper;
    private final CustomerTransactionHelper transactionHelper;

    /**
     * Create a new customer with main account.
     * Retries up to 5 times if a Luhn ID collision occurs (Customer ID or Account Number).
     *
     * <p>This method is @Retryable but intentionally NOT @Transactional.
     * The transactional work is delegated to {@link CustomerTransactionHelper},
     * ensuring each retry attempt gets a fresh transaction and clean persistence
     * context. A DuplicateResourceException (email conflict) is NOT retried
     * because it is not a DataIntegrityViolationException from a Luhn collision.</p>
     *
     * @param request the customer creation request
     * @return the created customer response
     * @throws DuplicateResourceException if email already exists (fails immediately, no retry)
     */
    @Retryable(
            retryFor = DataIntegrityViolationException.class,
            noRetryFor = DuplicateResourceException.class,
            maxAttempts = 5,
            backoff = @Backoff(delay = 100)
    )
    public CustomerDetailedResponse createCustomer(CreateCustomerRequest request) {
        return transactionHelper.executeCreateCustomer(request);
    }

    /**
     * Get customer by customer ID.
     *
     * @param customerId the customer ID
     * @return the customer response with accounts
     * @throws ResourceNotFoundException if customer not found
     */
    @Transactional(readOnly = true)
    public CustomerDetailedResponse getCustomer(Long customerId) {
        log.info("Fetching customer with ID: {}", customerId);

        return customerRepository.findByIdWithAccounts(customerId)
                .map(customerMapper::toDetailedResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with ID: " + customerId));
    }

    /**
     * Get a paginated list of all active customers.
     *
     * @param pageable pagination and sort parameters (page, size, sort)
     * @return page of customer summary responses
     */
    @Transactional(readOnly = true)
    public Page<CustomerSummaryResponse> getAllCustomers(Pageable pageable) {
        log.info("Fetching customers - page: {}, size: {}", pageable.getPageNumber(), pageable.getPageSize());

        return customerRepository.findAll(pageable)
                .map(customerMapper::toSummaryResponse);
    }

    /**
     * Search customers by mobile number fragment.
     * Returns customers whose mobile number contains the given string.
     *
     * @param mobileNumber the mobile number fragment to search for
     * @param pageable     pagination and sort parameters
     * @return page of matching customer summary responses
     */
    @Transactional(readOnly = true)
    public Page<CustomerSummaryResponse> searchByMobileNumber(String mobileNumber, Pageable pageable) {
        log.info("Searching customers by mobile number containing: {}", mobileNumber);

        return customerRepository.findByMobileNumberContaining(mobileNumber, pageable)
                .map(customerMapper::toSummaryResponse);
    }

    /**
     * Search customers by surname fragment (case-insensitive).
     * Returns customers whose last name contains the given string.
     *
     * @param surname  the surname fragment to search for
     * @param pageable pagination and sort parameters
     * @return page of matching customer summary responses
     */
    @Transactional(readOnly = true)
    public Page<CustomerSummaryResponse> searchBySurname(String surname, Pageable pageable) {
        log.info("Searching customers by surname containing: {}", surname);

        return customerRepository.findByLastNameContainingIgnoreCase(surname, pageable)
                .map(customerMapper::toSummaryResponse);
    }

    /**
     * Update an existing customer.
     *
     * @param customerId the customer ID
     * @param request    the update request
     * @return the updated customer response with accounts
     * @throws ResourceNotFoundException  if customer not found
     * @throws DuplicateResourceException if new email already exists
     */
    @Transactional
    public CustomerDetailedResponse updateCustomer(Long customerId, UpdateCustomerRequest request) {
        log.info("Updating customer with ID: {}", customerId);
        log.debug("Update request: {}", request);

        Customer customer = customerRepository.findByIdWithAccounts(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with ID: " + customerId));

        if (request.email() != null) {
            String normalizedNewEmail = customerMapper.normalizeEmail(request.email());
            if (!normalizedNewEmail.equals(customer.getEmail())) {
                if (customerRepository.existsByEmailIncludingInactive(normalizedNewEmail)) {
                    throw new DuplicateResourceException("Customer with email " + request.email() + " already exists");
                }
            }
        }

        customerMapper.updateEntity(request, customer);

        // Manually set updatedAt — @PreUpdate may not fire if Hibernate detects no
        // dirty fields after MapStruct applies identical values, or if the only change
        // is to updatedAt itself. Explicit assignment guarantees the timestamp updates.
        customer.setUpdatedAt(Instant.now());

        Customer updatedCustomer = customerRepository.save(customer);

        log.info("Customer updated successfully with ID: {}", customerId);

        return customerMapper.toDetailedResponse(updatedCustomer);
    }

    /**
     * Soft-delete a customer by setting active = false.
     * The @SQLRestriction on the entity filters this customer from all future queries.
     *
     * @param customerId the customer ID
     * @throws ResourceNotFoundException if customer not found
     */
    @Transactional
    public void deleteCustomer(Long customerId) {
        log.info("Soft-deleting customer with ID: {}", customerId);

        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found: " + customerId));

        customer.setActive(false);

        log.info("Customer soft-deleted successfully with ID: {}", customerId);
    }
}