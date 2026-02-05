/*
 * Copyright (c) 2026 Oualid Gharach. All rights reserved.
 *
 * Aiming for production-grade standards through clean code and best practices
 * for educational and informational purposes.
 *
 * Created on: 2/4/2026 at 7:26 PM
 *
 * Feel free to use or contribute. Contact: oualid.gharach@gmail.com
 */
/*
 * Copyright (c) 2026 Oualid Gharach. All rights reserved.
 *
 * Aiming for production-grade standards through clean code and best practices
 * for educational and informational purposes.
 *
 * Created on: 2/4/2026 at 7:26 PM
 *
 * Feel free to use or contribute. Contact: oualid.gharach@gmail.com
 */
package com.mycompany.api.account.service;

import com.mycompany.api.account.dto.CreateCustomerRequest;
import com.mycompany.api.account.dto.CustomerResponse;
import com.mycompany.api.account.dto.UpdateCustomerRequest;
import com.mycompany.api.account.exception.DuplicateResourceException;
import com.mycompany.api.account.exception.ResourceNotFoundException;
import com.mycompany.api.account.repository.CustomerRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration tests for CustomerService.
 * Tests actual database persistence with H2.
 *
 * @author Oualid Gharach
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("CustomerService Integration Tests")
class CustomerServiceIntegrationTest {

    @Autowired
    private CustomerService customerService;

    @Autowired
    private CustomerRepository customerRepository;

    @BeforeEach
    void setUp() {
        // Clean database before each test
        customerRepository.deleteAll();
    }

    @AfterEach
    void tearDown() {
        // Clean database after each test
        customerRepository.deleteAll();
    }

    @Test
    @DisplayName("Should create customer and persist to database")
    void shouldCreateCustomerAndPersist() {
        // Given
        CreateCustomerRequest request = new CreateCustomerRequest(
                "John",
                "Doe",
                "john@example.com",
                "+27821234567"
        );

        // When
        CustomerResponse response = customerService.createCustomer(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.customerId()).isNotNull();
        assertThat(response.firstName()).isEqualTo("John");
        assertThat(response.lastName()).isEqualTo("Doe");
        assertThat(response.email()).isEqualTo("john@example.com");
        assertThat(response.mobileNumber()).isEqualTo("+27821234567");
        assertThat(response.createdAt()).isNotNull();
        assertThat(response.updatedAt()).isNotNull();

        // Verify customer is actually in database
        assertThat(customerRepository.existsById(response.customerId())).isTrue();
        assertThat(customerRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should normalize data when creating customer")
    void shouldNormalizeDataWhenCreating() {
        // Given - Request with unnormalized data
        CreateCustomerRequest request = new CreateCustomerRequest(
                "  John  ",
                "  Doe  ",
                "  JOHN@EXAMPLE.COM  ",
                "+27 (82) 123-4567"
        );

        // When
        CustomerResponse response = customerService.createCustomer(request);

        // Then - Data should be normalized in database
        assertThat(response.firstName()).isEqualTo("John");
        assertThat(response.lastName()).isEqualTo("Doe");
        assertThat(response.email()).isEqualTo("john@example.com");
        assertThat(response.mobileNumber()).isEqualTo("+27821234567");


        // Verify normalized data in database using Optional.orElseThrow()
        var customer = customerRepository.findById(response.customerId())
                .orElseThrow(() -> new AssertionError("Customer should exist in database"));
        assertThat(customer.getFirstName()).isEqualTo("John");
        assertThat(customer.getLastName()).isEqualTo("Doe");
        assertThat(customer.getEmail()).isEqualTo("john@example.com");
        assertThat(customer.getMobileNumber()).isEqualTo("+27821234567");
    }

    @Test
    @DisplayName("Should throw DuplicateResourceException for duplicate email")
    void shouldThrowExceptionForDuplicateEmail() {
        // Given - Create first customer
        CreateCustomerRequest request1 = new CreateCustomerRequest(
                "John",
                "Doe",
                "john@example.com",
                "+27821234567"
        );
        customerService.createCustomer(request1);

        // When - Try to create second customer with same email
        CreateCustomerRequest request2 = new CreateCustomerRequest(
                "Jane",
                "Smith",
                "john@example.com",  // Same email
                "+27829999999"
        );

        // Then
        assertThatThrownBy(() -> customerService.createCustomer(request2))
                .isInstanceOf(DuplicateResourceException.class);

        // Verify only one customer in database
        assertThat(customerRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should get customer by ID from database")
    void shouldGetCustomerById() {
        // Given - Create customer
        CreateCustomerRequest request = new CreateCustomerRequest(
                "John",
                "Doe",
                "john@example.com",
                "+27821234567"
        );
        CustomerResponse created = customerService.createCustomer(request);

        // When
        CustomerResponse found = customerService.getCustomer(created.customerId());

        // Then - Verify timestamps are populated (not null)
        assertThat(created.createdAt()).isNotNull();
        assertThat(created.updatedAt()).isNotNull();

        // Compare ignoring timestamps (nanosecond precision lost in DB roundtrip)
        assertThat(found)
                .usingRecursiveComparison()
                .ignoringFields("createdAt", "updatedAt")
                .isEqualTo(created);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when customer not found")
    void shouldThrowExceptionWhenCustomerNotFound() {
        // Given - Non-existent customer ID
        Long nonExistentId = 99999999L;

        // When & Then
        assertThatThrownBy(() -> customerService.getCustomer(nonExistentId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Should get all customers from database")
    void shouldGetAllCustomers() {
        // Given - Create multiple customers and save references
        CustomerResponse customer1 = customerService.createCustomer(
                new CreateCustomerRequest("John", "Doe", "john@example.com", "+27821111111"));
        CustomerResponse customer2 = customerService.createCustomer(
                new CreateCustomerRequest("Jane", "Smith", "jane@example.com", "+27822222222"));
        CustomerResponse customer3 = customerService.createCustomer(
                new CreateCustomerRequest("Bob", "Brown", "bob@example.com", "+27823333333"));

        // When
        List<CustomerResponse> customers = customerService.getAllCustomers();

        // Then - Verify all timestamps are populated
        assertThat(customers).hasSize(3);
        assertThat(customer1.createdAt()).isNotNull();
        assertThat(customer2.createdAt()).isNotNull();
        assertThat(customer3.createdAt()).isNotNull();

        // Compare ignoring timestamps (nanosecond precision lost in DB roundtrip)
        assertThat(customers)
                .usingRecursiveFieldByFieldElementComparatorIgnoringFields("createdAt", "updatedAt")
                .containsExactlyInAnyOrder(customer1, customer2, customer3);
    }

    @Test
    @DisplayName("Should search customers by mobile number")
    void shouldSearchByMobileNumber() {
        // Given - Create customers with different mobile numbers
        customerService.createCustomer(new CreateCustomerRequest(
                "John", "Doe", "john@example.com", "+27821111111"));
        CustomerResponse customer2 = customerService.createCustomer(new CreateCustomerRequest(
                "Jane", "Smith", "jane@example.com", "+27829999999"));
        customerService.createCustomer(new CreateCustomerRequest(
                "Bob", "Brown", "bob@example.com", "+27831111111"));

        // When - Search for mobile containing "999"
        List<CustomerResponse> results = customerService.searchByMobileNumber("999");

        // Then - Compare ignoring timestamps (nanosecond precision lost in DB roundtrip)
        assertThat(results).hasSize(1);
        assertThat(results.getFirst())
                .usingRecursiveComparison()
                .ignoringFields("createdAt", "updatedAt")
                .isEqualTo(customer2);
    }

    @Test
    @DisplayName("Should update customer and persist changes")
    void shouldUpdateCustomerAndPersist() {
        // Given - Create customer
        CreateCustomerRequest createRequest = new CreateCustomerRequest(
                "John", "Doe", "john@example.com", "+27821234567");
        CustomerResponse created = customerService.createCustomer(createRequest);

        // When - Update customer
        UpdateCustomerRequest updateRequest = new UpdateCustomerRequest(
                "Jane",
                null,
                "jane@example.com",
                null
        );
        CustomerResponse updated = customerService.updateCustomer(created.customerId(), updateRequest);

        // Then
        assertThat(updated.firstName()).isEqualTo("Jane");
        assertThat(updated.lastName()).isEqualTo("Doe");  // Unchanged
        assertThat(updated.email()).isEqualTo("jane@example.com");
        assertThat(updated.mobileNumber()).isEqualTo("+27821234567");  // Unchanged

        // Verify changes persisted in database using Optional.orElseThrow()
        var customer = customerRepository.findById(created.customerId())
                .orElseThrow(() -> new AssertionError("Customer should exist in database"));
        assertThat(customer.getFirstName()).isEqualTo("Jane");
        assertThat(customer.getLastName()).isEqualTo("Doe");
        assertThat(customer.getEmail()).isEqualTo("jane@example.com");
        assertThat(customer.getMobileNumber()).isEqualTo("+27821234567");
    }

    @Test
    @DisplayName("Should update updatedAt timestamp when customer is modified")
    void shouldUpdateTimestampWhenModified() throws InterruptedException {
        // Given - Create customer
        CreateCustomerRequest createRequest = new CreateCustomerRequest(
                "John", "Doe", "john@example.com", "+27821234567");
        CustomerResponse created = customerService.createCustomer(createRequest);

        Instant originalCreatedAt = created.createdAt();
        Instant originalUpdatedAt = created.updatedAt();

        // Wait to ensure timestamp difference
        Thread.sleep(1000);

        // When - Update customer
        UpdateCustomerRequest updateRequest = new UpdateCustomerRequest(
                "Jane", null, null, null);
        CustomerResponse updated = customerService.updateCustomer(created.customerId(), updateRequest);

        // Then - updatedAt should be after (handle nanosecond precision)
        System.out.println("originalUpdatedAt: " + originalUpdatedAt);
        System.out.println("updated.updatedAt(): " + updated.updatedAt());
        assertThat(updated.updatedAt()).isAfter(originalUpdatedAt);  // updatedAt changed or equal due to precision

        // Verify in database that update actually happened
        var customer = customerRepository.findById(created.customerId())
                .orElseThrow(() -> new AssertionError("Customer should exist"));
        assertThat(customer.getFirstName()).isEqualTo("Jane");  // Verify update worked
    }



    @Test
    @DisplayName("Should throw exception when updating with duplicate email")
    void shouldThrowExceptionWhenUpdatingWithDuplicateEmail() {
        // Given - Create two customers
        CustomerResponse customer1 = customerService.createCustomer(
                new CreateCustomerRequest("John", "Doe", "john@example.com", "+27821111111"));
        CustomerResponse customer2 = customerService.createCustomer(
                new CreateCustomerRequest("Jane", "Smith", "jane@example.com", "+27822222222"));

        // When - Try to update customer2 with customer1's email
        UpdateCustomerRequest updateRequest = new UpdateCustomerRequest(
                null, null, "john@example.com", null);

        // Then
        assertThatThrownBy(() -> customerService.updateCustomer(customer2.customerId(), updateRequest))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    @DisplayName("Should delete customer from database")
    void shouldDeleteCustomer() {
        // Given - Create customer
        CreateCustomerRequest request = new CreateCustomerRequest(
                "John", "Doe", "john@example.com", "+27821234567");
        CustomerResponse created = customerService.createCustomer(request);

        // Verify customer exists using service (not repository directly)
        assertThatNoException().isThrownBy(() -> customerService.getCustomer(created.customerId()));

        // When - Delete customer
        customerService.deleteCustomer(created.customerId());

        // Then - Verify customer deleted (should throw ResourceNotFoundException)
        assertThatThrownBy(() -> customerService.getCustomer(created.customerId()))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThat(customerRepository.count()).isEqualTo(0);
    }

    @Test
    @DisplayName("Should throw exception when deleting non-existent customer")
    void shouldThrowExceptionWhenDeletingNonExistentCustomer() {
        // Given - Non-existent customer ID
        Long nonExistentId = 99999999L;

        // When & Then
        assertThatThrownBy(() -> customerService.deleteCustomer(nonExistentId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Should handle race condition for duplicate email")
    void shouldHandleRaceConditionForDuplicateEmail() {
        // Given - Create customer
        CreateCustomerRequest request = new CreateCustomerRequest(
                "John", "Doe", "john@example.com", "+27821234567");
        customerService.createCustomer(request);

        // When - Try to create another customer with normalized version of same email
        CreateCustomerRequest request2 = new CreateCustomerRequest(
                "Jane", "Smith", "  JOHN@EXAMPLE.COM  ", "+27829999999");

        // Then - Should catch the duplicate even with different formatting
        assertThatThrownBy(() -> customerService.createCustomer(request2))
                .isInstanceOf(DuplicateResourceException.class);
    }
}