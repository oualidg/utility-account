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
import com.mycompany.api.account.dto.CustomerResponse;
import com.mycompany.api.account.dto.UpdateCustomerRequest;
import com.mycompany.api.account.entity.Customer;
import com.mycompany.api.account.exception.DuplicateResourceException;
import com.mycompany.api.account.exception.ResourceNotFoundException;
import com.mycompany.api.account.mapper.CustomerMapper;
import com.mycompany.api.account.repository.CustomerRepository;
import com.mycompany.api.account.util.LuhnGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

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

    /**
     * Create a new customer.
     *
     * @param request the customer creation request
     * @return the created customer response
     * @throws DuplicateResourceException if email already exists
     */
    @Transactional
    public CustomerResponse createCustomer(CreateCustomerRequest request) {
        log.info("Creating customer with email: {}", request.email());
        log.debug("Full request: {}", request);

        // Map request to entity (with normalization) - happens once
        Customer customer = customerMapper.toEntity(request);

        // Save customer with retry - checks email on each attempt
        Customer savedCustomer = saveCustomerWithRetry(customer);

        // TODO: Phase 2 - Create default Account for this customer here
        // Account account = accountService.createDefaultAccount(savedCustomer);

        log.info("Customer created successfully with ID: {}", savedCustomer.getCustomerId());

        return customerMapper.toResponse(savedCustomer);
    }

    /**
     * Save customer with retry on collision.
     * Checks email and attempts save. Retries up to 3 times if customer ID collision occurs.
     * Email check happens on EVERY attempt to handle race conditions.
     *
     * @param customer the customer entity (already normalized)
     * @return the saved customer entity (for further processing)
     * @throws DuplicateResourceException if email already exists (no retry - fails immediately)
     */
    @Retryable(
            retryFor = DataIntegrityViolationException.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 100)
    )
    private Customer saveCustomerWithRetry(Customer customer) {
        // Check email on EVERY attempt (handles race conditions)
        if (customerRepository.existsByEmail(customer.getEmail())) {
            throw new DuplicateResourceException("Customer with email " + customer.getEmail() + " already exists");
        }

        // Generate customer ID on each attempt
        Long customerId = LuhnGenerator.generateCustomerId();
        customer.setCustomerId(customerId);

        // Save to database (will throw DataIntegrityViolationException if ID collision)
        return customerRepository.save(customer);
    }

    /**
     * Get customer by customer ID.
     *
     * @param customerId the customer ID
     * @return the customer response
     * @throws ResourceNotFoundException if customer not found
     */
    @Transactional(readOnly = true)
    public CustomerResponse getCustomer(Long customerId) {
        log.info("Fetching customer with ID: {}", customerId);

        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with ID: " + customerId));

        return customerMapper.toResponse(customer);
    }

    /**
     * Get all customers.
     *
     * @return list of all customers
     */
    @Transactional(readOnly = true)
    public List<CustomerResponse> getAllCustomers() {
        log.info("Fetching all customers");

        return customerRepository.findAll().stream()
                .map(customerMapper::toResponse)
                .toList();
    }

    /**
     * Search customers by mobile number containing the given string.
     *
     * @param mobileNumber the mobile number string to search for
     * @return list of matching customers
     */
    @Transactional(readOnly = true)
    public List<CustomerResponse> searchByMobileNumber(String mobileNumber) {
        log.info("Searching customers by mobile number containing: {}", mobileNumber);

        return customerRepository.findByMobileNumberContaining(mobileNumber).stream()
                .map(customerMapper::toResponse)
                .toList();
    }

    /**
     * Update an existing customer.
     *
     * @param customerId the customer ID
     * @param request the update request
     * @return the updated customer response
     * @throws ResourceNotFoundException if customer not found
     * @throws DuplicateResourceException if new email already exists
     */
    @Transactional
    public CustomerResponse updateCustomer(Long customerId, UpdateCustomerRequest request) {
        log.info("Updating customer with ID: {}", customerId);
        log.debug("Update request: {}", request);

        // Find existing customer
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with ID: " + customerId));

        // Check if email is being changed and if new email already exists
        if (request.email() != null) {
            String normalizedNewEmail = customerMapper.normalizeEmail(request.email());
            if (!normalizedNewEmail.equals(customer.getEmail())) {
                if (customerRepository.existsByEmail(normalizedNewEmail)) {
                    throw new DuplicateResourceException("Customer with email " + request.email() + " already exists");
                }
            }
        }

        // Update entity using MapStruct (only non-null fields, with normalization)
        customerMapper.updateEntity(request, customer);

        // Manually set updatedAt to ensure it's updated (JPA dirty checking doesn't always detect MapStruct changes)
        customer.setUpdatedAt(Instant.now());

        // Save updated customer
        Customer updatedCustomer = customerRepository.save(customer);

        log.info("Customer updated successfully with ID: {}", customerId);

        return customerMapper.toResponse(updatedCustomer);
    }

    /**
     * Delete a customer.
     *
     * @param customerId the customer ID
     * @throws ResourceNotFoundException if customer not found
     */
    @Transactional
    public void deleteCustomer(Long customerId) {
        log.info("Deleting customer with ID: {}", customerId);

        if (!customerRepository.existsById(customerId)) {
            throw new ResourceNotFoundException("Customer not found with ID: " + customerId);
        }

        customerRepository.deleteById(customerId);

        log.info("Customer deleted successfully with ID: {}", customerId);
    }
}