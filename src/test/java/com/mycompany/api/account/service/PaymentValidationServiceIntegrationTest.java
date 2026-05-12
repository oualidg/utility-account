/*
 * Copyright (c) 2026 Oualid Gharach. All rights reserved.
 *
 * Aiming for production-grade standards through clean code and best practices
 * for educational and informational purposes.
 *
 * Created on: 5/11/2026 at 9:01 PM
 *
 * Feel free to use or contribute. Contact: oualid.gharach@gmail.com
 */
package com.mycompany.api.account.service;

import com.mycompany.api.account.BaseIntegrationTest;
import com.mycompany.api.account.entity.Account;
import com.mycompany.api.account.entity.Customer;
import com.mycompany.api.account.repository.AccountRepository;
import com.mycompany.api.account.repository.CustomerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for PaymentValidationService.
 *
 * <p>Verifies account and customer validation logic against a real PostgreSQL
 * instance via Testcontainers. Covers active, inactive, and missing entity
 * scenarios to ensure the service correctly delegates to repository queries.</p>
 *
 * @author Oualid Gharach
 */
@DisplayName("PaymentValidationService Integration Tests")
class PaymentValidationServiceIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private PaymentValidationService paymentValidationService;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Customer activeCustomer;
    private Account mainAccount;

    @BeforeEach
    void setUp() {
        // Hard delete via native SQL — bypasses @SQLRestriction on Customer
        jdbcTemplate.execute("DELETE FROM payments");
        jdbcTemplate.execute("DELETE FROM accounts");
        jdbcTemplate.execute("DELETE FROM customers");

        activeCustomer = seedCustomer(12345674L, "John", "Doe", "john.doe@example.com");
        mainAccount = seedAccount(1234567897L, activeCustomer, true);
    }

    // =========================================================================
    // isAccountValid
    // =========================================================================

    @Test
    @DisplayName("Should return true for an active account with an active customer")
    void shouldReturnTrueForActiveAccount() {
        assertThat(paymentValidationService.isAccountValid(mainAccount.getAccountNumber())).isTrue();
    }

    @Test
    @DisplayName("Should return false when account number does not exist")
    void shouldReturnFalseWhenAccountNotFound() {
        assertThat(paymentValidationService.isAccountValid(9999999999L)).isFalse();
    }

    @Test
    @DisplayName("Should return false when account belongs to an inactive customer")
    void shouldReturnFalseWhenAccountBelongsToInactiveCustomer() {
        jdbcTemplate.execute("UPDATE customers SET active = false WHERE customer_id = "
                + activeCustomer.getCustomerId());

        assertThat(paymentValidationService.isAccountValid(mainAccount.getAccountNumber())).isFalse();
    }

    // =========================================================================
    // isCustomerValid
    // =========================================================================

    @Test
    @DisplayName("Should return true when customer is active and has a main account")
    void shouldReturnTrueForActiveCustomerWithMainAccount() {
        assertThat(paymentValidationService.isCustomerValid(activeCustomer.getCustomerId())).isTrue();
    }

    @Test
    @DisplayName("Should return false when customer ID does not exist")
    void shouldReturnFalseWhenCustomerIdNotFound() {
        assertThat(paymentValidationService.isCustomerValid(99999999L)).isFalse();
    }

    @Test
    @DisplayName("Should return false when customer has no main account")
    void shouldReturnFalseWhenCustomerHasNoMainAccount() {
        Customer noMainCustomer = seedCustomer(87654321L, "Jane", "Smith", "jane.smith@example.com");
        seedAccount(8765432109L, noMainCustomer, false);

        assertThat(paymentValidationService.isCustomerValid(noMainCustomer.getCustomerId())).isFalse();
    }

    @Test
    @DisplayName("Should return false when customer is inactive")
    void shouldReturnFalseWhenCustomerIsInactive() {
        // Native SQL to bypass @SQLRestriction
        jdbcTemplate.execute("UPDATE customers SET active = false WHERE customer_id = "
                + activeCustomer.getCustomerId());

        assertThat(paymentValidationService.isCustomerValid(activeCustomer.getCustomerId())).isFalse();
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private Customer seedCustomer(Long id, String firstName, String lastName,
                                  String email) {
        Customer customer = new Customer();
        customer.setCustomerId(id);
        customer.setFirstName(firstName);
        customer.setLastName(lastName);
        customer.setEmail(email);
        customer.setMobileNumber("0821234567");
        customer.setActive(true);
        return customerRepository.save(customer);
    }

    private Account seedAccount(Long accountNumber, Customer customer, boolean isMain) {
        Account account = new Account();
        account.setAccountNumber(accountNumber);
        account.setCustomer(customer);
        account.setBalance(BigDecimal.ZERO);
        account.setMainAccount(isMain);
        return accountRepository.save(account);
    }
}