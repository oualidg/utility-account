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
import com.mycompany.api.account.dto.CustomerDetailedResponse;
import com.mycompany.api.account.dto.CustomerSummaryResponse;
import com.mycompany.api.account.dto.UpdateCustomerRequest;
import com.mycompany.api.account.exception.DuplicateResourceException;
import com.mycompany.api.account.exception.ResourceNotFoundException;
import com.mycompany.api.account.repository.AccountRepository;
import com.mycompany.api.account.repository.CustomerRepository;
import com.mycompany.api.account.util.LuhnGenerator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
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

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        // Hard delete all data before each test using native SQL to bypass soft delete
        jdbcTemplate.execute("DELETE FROM payments");
        jdbcTemplate.execute("DELETE FROM accounts");
        jdbcTemplate.execute("DELETE FROM customers");
    }

    @AfterEach
    void tearDown() {
        // Hard delete all data after each test using native SQL to bypass soft delete
        jdbcTemplate.execute("DELETE FROM payments");
        jdbcTemplate.execute("DELETE FROM accounts");
        jdbcTemplate.execute("DELETE FROM customers");
    }

    @Test
    @DisplayName("Should create customer and normalize customer data during creation and persist to database")
    void shouldCreateCustomerAndNormalizeCustomerDataAndPersist() {
        // Given - Request with data that needs normalization
        CreateCustomerRequest request = new CreateCustomerRequest(
                "  John  ",              // Extra spaces
                "  Doe  ",               // Extra spaces
                "  JOHN@EXAMPLE.COM  ",  // Uppercase with spaces
                "+27 (82) 123-4567"      // With formatting
        );

        // When
        CustomerDetailedResponse response = customerService.createCustomer(request);

        // Then - Verify customer and account details
        assertThat(response).isNotNull();
        assertThat(response.customerId()).isNotNull();
        String customerIdStr = response.customerId().toString();
        assertThat(customerIdStr).hasSize(8); // Verify customer ID is 8 digits
        assertThat(LuhnGenerator.isValidCustomerId(response.customerId().toString())).isTrue(); // Verify Luhn checksum (manually validated)
        assertThat(response.firstName()).isEqualTo("John");
        assertThat(response.lastName()).isEqualTo("Doe");
        assertThat(response.email()).isEqualTo("john@example.com");
        assertThat(response.mobileNumber()).isEqualTo("+27821234567");
        assertThat(response.accounts()).isNotNull();
        assertThat(response.accounts()).hasSize(1);
        assertThat(response.accounts().getFirst().isMainAccount()).isTrue();
        assertThat(response.accounts().getFirst().balance()).isZero();
        String accountNumberStr = response.accounts().getFirst().accountNumber().toString();
        assertThat(accountNumberStr).hasSize(10); // Verify account number is 10 digits
        assertThat(LuhnGenerator.isValidAccountNumber(response.accounts().getFirst().accountNumber().toString())).isTrue(); // Verify Luhn checksum (manually validated)

        // Verify timestamps
        assertThat(response.createdAt()).isNotNull();
        assertThat(response.updatedAt()).isNotNull();

        // Verify customer is in database
        assertThat(customerRepository.count()).isEqualTo(1);

        // Verify account is in database
        assertThat(accountRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should throw exception when creating customer with duplicate email")
    void shouldThrowExceptionOnDuplicateEmail() {
        // Given - Create first customer
        CreateCustomerRequest request1 = new CreateCustomerRequest(
                "John", "Doe", "john@example.com", "+27821234567");
        customerService.createCustomer(request1);

        // When - Try to create another customer with same email
        CreateCustomerRequest request2 = new CreateCustomerRequest(
                "Jane", "Smith", "  JOHN@EXAMPLE.COM  ", "+27829999999");

        // Then
        assertThatThrownBy(() -> customerService.createCustomer(request2))
                .isInstanceOf(DuplicateResourceException.class);
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

        // When
        CustomerDetailedResponse created = customerService.createCustomer(request);
        CustomerDetailedResponse found = customerService.getCustomer(created.customerId());

        // Then - Verify timestamps are populated (not null)
        assertThat(created.createdAt()).isNotNull();
        assertThat(created.updatedAt()).isNotNull();
        assertThat(found.createdAt()).isNotNull();
        assertThat(found.updatedAt()).isNotNull();

        // Compare ignoring all timestamps (Instant) and BigDecimal
        assertThat(found)
                .usingRecursiveComparison()
                .ignoringFieldsOfTypes(Instant.class, BigDecimal.class)
                .isEqualTo(created);

        // Verify account balance separately (BigDecimal comparison)
        assertThat(found.accounts().getFirst().balance())
                .isEqualByComparingTo(created.accounts().getFirst().balance());
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
        // Given - Create multiple customers
        customerService.createCustomer(new CreateCustomerRequest(
                "John", "Doe", "john@example.com", "+27821234567"));
        customerService.createCustomer(new CreateCustomerRequest(
                "Jane", "Smith", "jane@example.com", "+27829999999"));

        // When
        List<CustomerSummaryResponse> customers = customerService.getAllCustomers();

        // Then
        assertThat(customers).hasSize(2);
        assertThat(customers).extracting("firstName").containsExactlyInAnyOrder("John", "Jane");

    }

    @Test
    @DisplayName("Should search customers by mobile number")
    void shouldSearchByMobileNumber() {
        // Given - Create customers with different mobile numbers
        customerService.createCustomer(new CreateCustomerRequest(
                "John", "Doe", "john@example.com", "+27821234567"));
        customerService.createCustomer(new CreateCustomerRequest(
                "Jane", "Smith", "jane@example.com", "+27829999999"));

        // When - Search for "821"
        List<CustomerSummaryResponse> results = customerService.searchByMobileNumber("821");

        // Then - Should find John
        assertThat(results).hasSize(1);
        assertThat(results.getFirst().firstName()).isEqualTo("John");
    }

    @Test
    @DisplayName("Should update customer and persist changes")
    void shouldUpdateCustomer() {
        // Given - Create customer
        CreateCustomerRequest createRequest = new CreateCustomerRequest(
                "John", "Doe", "john@example.com", "+27821234567");
        CustomerDetailedResponse created = customerService.createCustomer(createRequest);

        // When - Update customer
        UpdateCustomerRequest updateRequest = new UpdateCustomerRequest(
                "Jane", "Smith", "jane@example.com", "+27829999999");
        CustomerDetailedResponse updated = customerService.updateCustomer(created.customerId(), updateRequest);

        // Then
        assertThat(updated.customerId()).isEqualTo(created.customerId());
        assertThat(updated.firstName()).isEqualTo("Jane");
        assertThat(updated.lastName()).isEqualTo("Smith");
        assertThat(updated.email()).isEqualTo("jane@example.com");
        assertThat(updated.mobileNumber()).isEqualTo("+27829999999");
        assertThat(updated.updatedAt()).isAfter(created.updatedAt());
    }

    @Test
    @DisplayName("Should update only provided fields (partial update)")
    void shouldPartiallyUpdateCustomer() {
        // Given - Create customer
        CreateCustomerRequest createRequest = new CreateCustomerRequest(
                "John", "Doe", "john@example.com", "+27821234567");
        CustomerDetailedResponse created = customerService.createCustomer(createRequest);

        // When - Update only first name
        UpdateCustomerRequest updateRequest = new UpdateCustomerRequest(
                "Jane", null, null, null);
        CustomerDetailedResponse updated = customerService.updateCustomer(created.customerId(), updateRequest);

        // Then - Only first name changed
        assertThat(updated.firstName()).isEqualTo("Jane");
        assertThat(updated.lastName()).isEqualTo("Doe");  // Unchanged
        assertThat(updated.email()).isEqualTo("john@example.com");  // Unchanged
        assertThat(updated.mobileNumber()).isEqualTo("+27821234567");  // Unchanged
        assertThat(updated.updatedAt()).isAfter(created.updatedAt());
    }

    @Test
    @DisplayName("Should throw exception when updating to duplicate email")
    void shouldThrowExceptionWhenUpdatingToDuplicateEmail() {
        // Given - Create two customers
        customerService.createCustomer(new CreateCustomerRequest(
                "John", "Doe", "john@example.com", "+27821234567"));
        CustomerDetailedResponse customer2 = customerService.createCustomer(new CreateCustomerRequest(
                "Jane", "Smith", "jane@example.com", "+27829999999"));

        // When - Try to update customer2's email to customer1's email
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
        CustomerDetailedResponse created = customerService.createCustomer(request);

        // Verify customer exists
        assertThatNoException().isThrownBy(() -> customerService.getCustomer(created.customerId()));

        // When - Delete customer (soft delete)
        customerService.deleteCustomer(created.customerId());

        // Then - Verify customer is deleted (not findable via service)
        // Soft delete sets active=false, so @SQLRestriction filters it out
        assertThatThrownBy(() -> customerService.getCustomer(created.customerId()))
                .isInstanceOf(ResourceNotFoundException.class);

        Long totalCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM customers", Long.class);
        assertThat(totalCount).isEqualTo(1);  // Soft deleted row still exists
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


}