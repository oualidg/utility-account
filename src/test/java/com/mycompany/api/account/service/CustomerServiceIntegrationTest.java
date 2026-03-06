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

import com.mycompany.api.account.BaseIntegrationTest;
import com.mycompany.api.account.dto.CreateCustomerRequest;
import com.mycompany.api.account.dto.CustomerDetailedResponse;
import com.mycompany.api.account.dto.CustomerSummaryResponse;
import com.mycompany.api.account.dto.UpdateCustomerRequest;
import com.mycompany.api.account.exception.DuplicateResourceException;
import com.mycompany.api.account.exception.ResourceNotFoundException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration tests for CustomerService.
 * Tests actual database persistence with Testcontainers PostgreSQL.
 *
 * @author Oualid Gharach
 */
@DisplayName("CustomerService Integration Tests")
class CustomerServiceIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private CustomerService customerService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // Default pageable used across list/search tests — page 0, size 10, newest first
    private static final Pageable DEFAULT_PAGE = PageRequest.of(0, 10,
            Sort.by(Sort.Direction.DESC, "createdAt"));

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("DELETE FROM payments");
        jdbcTemplate.execute("DELETE FROM accounts");
        jdbcTemplate.execute("DELETE FROM customers");
    }

    @AfterEach
    void tearDown() {
        jdbcTemplate.execute("DELETE FROM payments");
        jdbcTemplate.execute("DELETE FROM accounts");
        jdbcTemplate.execute("DELETE FROM customers");
    }

    // -------------------------------------------------------------------------
    // createCustomer
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Should create customer and normalize customer data during creation and persist to database")
    void shouldCreateCustomerAndNormalizeCustomerDataAndPersist() {
        CreateCustomerRequest request = new CreateCustomerRequest(
                "  John  ",
                "  Doe  ",
                "  JOHN@EXAMPLE.COM  ",
                "+27821234567"
        );

        CustomerDetailedResponse response = customerService.createCustomer(request);

        assertThat(response.customerId()).isNotNull();
        assertThat(response.firstName()).isEqualTo("John");
        assertThat(response.lastName()).isEqualTo("Doe");
        assertThat(response.email()).isEqualTo("john@example.com");
        assertThat(response.mobileNumber()).isEqualTo("+27821234567");
        assertThat(response.accounts()).hasSize(1);
        assertThat(response.createdAt()).isNotNull();
        assertThat(response.updatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should throw DuplicateResourceException when email already exists")
    void shouldThrowDuplicateResourceExceptionWhenEmailAlreadyExists() {
        CreateCustomerRequest first = new CreateCustomerRequest(
                "John", "Doe", "john@example.com", "+27821234567"
        );
        CreateCustomerRequest second = new CreateCustomerRequest(
                "Jane", "Smith", "john@example.com", "+27829999999"
        );

        customerService.createCustomer(first);

        assertThatThrownBy(() -> customerService.createCustomer(second))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("john@example.com");
    }

    // -------------------------------------------------------------------------
    // getCustomer
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Should return customer with accounts when found by ID")
    void shouldReturnCustomerWithAccountsWhenFoundById() {
        CustomerDetailedResponse created = customerService.createCustomer(
                new CreateCustomerRequest("John", "Doe", "john@example.com", "+27821234567")
        );

        CustomerDetailedResponse found = customerService.getCustomer(created.customerId());

        assertThat(found.customerId()).isEqualTo(created.customerId());
        assertThat(found.firstName()).isEqualTo("John");
        assertThat(found.accounts()).hasSize(1);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when customer not found by ID")
    void shouldThrowResourceNotFoundExceptionWhenCustomerNotFoundById() {
        assertThatThrownBy(() -> customerService.getCustomer(12345674L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("12345674");
    }

    // -------------------------------------------------------------------------
    // getAllCustomers
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Should return paginated customer list ordered by createdAt descending")
    void shouldReturnPaginatedCustomerListOrderedByCreatedAtDesc() {
        customerService.createCustomer(
                new CreateCustomerRequest("Alice", "Adams", "alice@example.com", "+27821111111")
        );
        customerService.createCustomer(
                new CreateCustomerRequest("Bob", "Brown", "bob@example.com", "+27822222222")
        );
        customerService.createCustomer(
                new CreateCustomerRequest("Carol", "Clark", "carol@example.com", "+27823333333")
        );

        Page<CustomerSummaryResponse> page = customerService.getAllCustomers(DEFAULT_PAGE);

        assertThat(page.getTotalElements()).isEqualTo(3);
        assertThat(page.getContent()).hasSize(3);
        assertThat(page.getTotalPages()).isEqualTo(1);
        assertThat(page.getNumber()).isEqualTo(0);
        // Most recently created customer (Carol) should appear first
        assertThat(page.getContent().getFirst().firstName()).isEqualTo("Carol");
    }

    @Test
    @DisplayName("Should return correct page when requesting second page")
    void shouldReturnCorrectPageWhenRequestingSecondPage() {
        for (int i = 1; i <= 12; i++) {
            customerService.createCustomer(new CreateCustomerRequest(
                    "First" + i, "Last" + i,
                    "customer" + i + "@example.com",
                    "+2782111111" + (i % 10)
            ));
        }

        Pageable secondPage = PageRequest.of(1, 10, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<CustomerSummaryResponse> page = customerService.getAllCustomers(secondPage);

        assertThat(page.getTotalElements()).isEqualTo(12);
        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getTotalPages()).isEqualTo(2);
        assertThat(page.getNumber()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should return empty page when no customers exist")
    void shouldReturnEmptyPageWhenNoCustomersExist() {
        Page<CustomerSummaryResponse> page = customerService.getAllCustomers(DEFAULT_PAGE);

        assertThat(page.getTotalElements()).isEqualTo(0);
        assertThat(page.getContent()).isEmpty();
    }

    // -------------------------------------------------------------------------
    // searchByMobileNumber
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Should return matching customers when searching by mobile fragment")
    void shouldReturnMatchingCustomersWhenSearchingByMobileFragment() {
        customerService.createCustomer(
                new CreateCustomerRequest("John", "Doe", "john@example.com", "+27821234567")
        );
        customerService.createCustomer(
                new CreateCustomerRequest("Jane", "Smith", "jane@example.com", "+27829999999")
        );

        Page<CustomerSummaryResponse> page = customerService.searchByMobileNumber("821", DEFAULT_PAGE);

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().getFirst().firstName()).isEqualTo("John");
    }

    @Test
    @DisplayName("Should return empty page when no mobile numbers match")
    void shouldReturnEmptyPageWhenNoMobileNumbersMatch() {
        customerService.createCustomer(
                new CreateCustomerRequest("John", "Doe", "john@example.com", "+27821234567")
        );

        Page<CustomerSummaryResponse> page = customerService.searchByMobileNumber("999", DEFAULT_PAGE);

        assertThat(page.getTotalElements()).isEqualTo(0);
        assertThat(page.getContent()).isEmpty();
    }

    // -------------------------------------------------------------------------
    // searchBySurname
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Should return matching customers when searching by surname fragment")
    void shouldReturnMatchingCustomersWhenSearchingBySurnameFragment() {
        customerService.createCustomer(
                new CreateCustomerRequest("John", "Doe", "john@example.com", "+27821234567")
        );
        customerService.createCustomer(
                new CreateCustomerRequest("Jane", "Donovan", "jane@example.com", "+27829999999")
        );
        customerService.createCustomer(
                new CreateCustomerRequest("Bob", "Smith", "bob@example.com", "+27823333333")
        );

        Page<CustomerSummaryResponse> page = customerService.searchBySurname("do", DEFAULT_PAGE);

        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getContent())
                .extracting(CustomerSummaryResponse::lastName)
                .containsExactlyInAnyOrder("Doe", "Donovan");
    }

    @Test
    @DisplayName("Should search surname case-insensitively")
    void shouldSearchSurnameCaseInsensitively() {
        customerService.createCustomer(
                new CreateCustomerRequest("John", "Doe", "john@example.com", "+27821234567")
        );

        Page<CustomerSummaryResponse> upperPage = customerService.searchBySurname("DOE", DEFAULT_PAGE);
        Page<CustomerSummaryResponse> lowerPage = customerService.searchBySurname("doe", DEFAULT_PAGE);
        Page<CustomerSummaryResponse> mixedPage = customerService.searchBySurname("DoE", DEFAULT_PAGE);

        assertThat(upperPage.getTotalElements()).isEqualTo(1);
        assertThat(lowerPage.getTotalElements()).isEqualTo(1);
        assertThat(mixedPage.getTotalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should return empty page when no surnames match")
    void shouldReturnEmptyPageWhenNoSurnamesMatch() {
        customerService.createCustomer(
                new CreateCustomerRequest("John", "Doe", "john@example.com", "+27821234567")
        );

        Page<CustomerSummaryResponse> page = customerService.searchBySurname("xyz", DEFAULT_PAGE);

        assertThat(page.getTotalElements()).isEqualTo(0);
        assertThat(page.getContent()).isEmpty();
    }

    // -------------------------------------------------------------------------
    // updateCustomer
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Should update customer fields successfully")
    void shouldUpdateCustomerFieldsSuccessfully() {
        CustomerDetailedResponse created = customerService.createCustomer(
                new CreateCustomerRequest("John", "Doe", "john@example.com", "+27821234567")
        );

        UpdateCustomerRequest update = new UpdateCustomerRequest(
                "Johnny", "Doe", "johnny@example.com", "+27821234567"
        );

        CustomerDetailedResponse updated = customerService.updateCustomer(created.customerId(), update);

        assertThat(updated.firstName()).isEqualTo("Johnny");
        assertThat(updated.email()).isEqualTo("johnny@example.com");
        assertThat(updated.updatedAt()).isAfterOrEqualTo(updated.createdAt());
    }

    @Test
    @DisplayName("Should throw DuplicateResourceException when updating to existing email")
    void shouldThrowDuplicateResourceExceptionWhenUpdatingToExistingEmail() {
        customerService.createCustomer(
                new CreateCustomerRequest("John", "Doe", "john@example.com", "+27821234567")
        );
        CustomerDetailedResponse second = customerService.createCustomer(
                new CreateCustomerRequest("Jane", "Smith", "jane@example.com", "+27829999999")
        );

        UpdateCustomerRequest update = new UpdateCustomerRequest(
                null, null, "john@example.com", null
        );

        assertThatThrownBy(() -> customerService.updateCustomer(second.customerId(), update))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when updating non-existent customer")
    void shouldThrowResourceNotFoundExceptionWhenUpdatingNonExistentCustomer() {
        UpdateCustomerRequest update = new UpdateCustomerRequest(
                "John", "Doe", "john@example.com", "+27821234567"
        );

        assertThatThrownBy(() -> customerService.updateCustomer(12345674L, update))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // -------------------------------------------------------------------------
    // deleteCustomer
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Should soft-delete customer and exclude from subsequent queries")
    void shouldSoftDeleteCustomerAndExcludeFromSubsequentQueries() {
        CustomerDetailedResponse created = customerService.createCustomer(
                new CreateCustomerRequest("John", "Doe", "john@example.com", "+27821234567")
        );

        customerService.deleteCustomer(created.customerId());

        Page<CustomerSummaryResponse> page = customerService.getAllCustomers(DEFAULT_PAGE);
        assertThat(page.getTotalElements()).isEqualTo(0);

        assertThatThrownBy(() -> customerService.getCustomer(created.customerId()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when deleting non-existent customer")
    void shouldThrowResourceNotFoundExceptionWhenDeletingNonExistentCustomer() {
        assertThatThrownBy(() -> customerService.deleteCustomer(12345674L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}