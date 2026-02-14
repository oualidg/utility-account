/*
 * Copyright (c) 2026 Oualid Gharach. All rights reserved.
 *
 * Aiming for production-grade standards through clean code and best practices
 * for educational and informational purposes.
 *
 * Created on: 2/13/2026 at 5:49 PM
 *
 * Feel free to use or contribute. Contact: oualid.gharach@gmail.com
 */
package com.mycompany.api.account.service;

import com.mycompany.api.account.entity.Account;
import com.mycompany.api.account.entity.Customer;
import com.mycompany.api.account.exception.DuplicateResourceException;
import com.mycompany.api.account.exception.ResourceNotFoundException;
import com.mycompany.api.account.repository.AccountRepository;
import com.mycompany.api.account.repository.CustomerRepository;
import com.mycompany.api.account.util.LuhnGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integration tests for AccountService.
 * Tests service layer with real database and repositories.
 *
 * @author Oualid Gharach
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("AccountService Integration Tests")
class AccountServiceIntegrationTest {

    @Autowired
    private AccountService accountService;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Customer testCustomer;

    @BeforeEach
    void setUp() {
        // Hard delete all data before each test using native SQL
        jdbcTemplate.execute("DELETE FROM payments");
        jdbcTemplate.execute("DELETE FROM accounts");
        jdbcTemplate.execute("DELETE FROM customers");

        // Create test customer
        testCustomer = new Customer();
        testCustomer.setCustomerId(12345674L);
        testCustomer.setFirstName("John");
        testCustomer.setLastName("Doe");
        testCustomer.setEmail("john@example.com");
        testCustomer.setMobileNumber("+27821234567");
        testCustomer = customerRepository.save(testCustomer);
    }

    // =========================================================================
    // createMainAccount Tests
    // =========================================================================

    @Test
    @DisplayName("Should create main account for customer successfully")
    void shouldCreateMainAccountForCustomer() {
        // When
        Account account = accountService.createMainAccount(testCustomer);

        // Then
        assertThat(account).isNotNull();
        assertThat(account.getAccountNumber()).isNotNull();
        assertThat(LuhnGenerator.isValidAccountNumber(account.getAccountNumber().toString())).isTrue(); // should pass Luhn validation
        assertThat(account.getCustomer().getCustomerId()).isEqualTo(testCustomer.getCustomerId());
        assertThat(account.getBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(account.isMainAccount()).isTrue();
        assertThat(account.getCreatedAt()).isNotNull();
        assertThat(account.getUpdatedAt()).isNotNull();

        // Verify persisted
        Account savedAccount = accountRepository.findById(account.getAccountNumber())
                .orElseThrow(() -> new AssertionError(
                        "Expected account " + account.getAccountNumber() + " to be persisted"
                ));
        assertThat(savedAccount.isMainAccount()).isTrue();
    }

    @Test
    @DisplayName("Should generate unique account numbers with Luhn checksum")
    void shouldGenerateUniqueAccountNumbersWithLuhn() {
        // When - Create multiple accounts
        Account account1 = accountService.createMainAccount(testCustomer);

        Customer customer2 = new Customer();
        customer2.setCustomerId(87654323L);
        customer2.setFirstName("Jane");
        customer2.setLastName("Smith");
        customer2.setEmail("jane@example.com");
        customer2.setMobileNumber("+27829999999");
        customer2 = customerRepository.save(customer2);

        Account account2 = accountService.createMainAccount(customer2);

        // Then - Account numbers should be different
        assertThat(account1.getAccountNumber()).isNotEqualTo(account2.getAccountNumber());

        // Both should pass Luhn validation
        assertThat(LuhnGenerator.isValidAccountNumber(account1.getAccountNumber().toString())).isTrue();
        assertThat(LuhnGenerator.isValidAccountNumber(account2.getAccountNumber().toString())).isTrue();
    }

    @Test
    @DisplayName("Should prevent multiple main accounts but allow secondary accounts for the same customer")
    void shouldThrowExceptionWhenDuplicateMainAccountForSameCustomer() {

        accountService.createMainAccount(testCustomer);

        // Act & Then
        // We attempt to create a second main account for the same testCustomer
        assertThatThrownBy(() -> accountService.createMainAccount(testCustomer))
                .as("The service should prevent a single customer from having multiple main accounts")
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("already has a main account")
                .hasMessageContaining(testCustomer.getCustomerId().toString());

        accountService.createAccount(testCustomer);

        // Verify that the database still only has exactly 2 accounts for this customer
        List<Account> accounts = accountRepository.findByCustomer_CustomerId(testCustomer.getCustomerId());
        assertThat(accounts).hasSize(2);
    }

    // =========================================================================
    // getMainAccount Tests
    // =========================================================================

    @Test
    @DisplayName("Should get main account by customer ID")
    void shouldGetMainAccountByCustomerId() {
        // Given
        Account createdAccount = accountService.createMainAccount(testCustomer);

        // When
        Account retrievedAccount = accountService.getMainAccount(testCustomer.getCustomerId());

        // Then
        assertThat(retrievedAccount).isNotNull();
        assertThat(retrievedAccount.getAccountNumber()).isEqualTo(createdAccount.getAccountNumber());
        assertThat(retrievedAccount.isMainAccount()).isTrue();
        assertThat(retrievedAccount.getCustomer().getCustomerId()).isEqualTo(testCustomer.getCustomerId());
    }

    @Test
    @DisplayName("Should throw exception when customer has no main account")
    void shouldThrowExceptionWhenCustomerHasNoMainAccount() {
        // Given - Customer with no accounts
        Customer customerWithoutAccount = new Customer();
        customerWithoutAccount.setCustomerId(99999997L);
        customerWithoutAccount.setFirstName("NoAccount");
        customerWithoutAccount.setLastName("User");
        customerWithoutAccount.setEmail("noaccoun@example.com");
        customerWithoutAccount.setMobileNumber("+27821111111");
        customerWithoutAccount = customerRepository.save(customerWithoutAccount);

        Long customerId = customerWithoutAccount.getCustomerId();

        // When & Then
        assertThatThrownBy(() -> accountService.getMainAccount(customerId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Main account not found for customer: " + customerId);
    }

    @Test
    @DisplayName("Should throw exception when getting main account of deleted customer")
    void shouldThrowExceptionWhenGettingMainAccountOfDeletedCustomer() {
        // Given - Create account then soft-delete customer
        accountService.createMainAccount(testCustomer);

        testCustomer.setActive(false);
        customerRepository.save(testCustomer);

        // When & Then - Should not find main account (customer is inactive)
        assertThatThrownBy(() -> accountService.getMainAccount(testCustomer.getCustomerId()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Main account not found");
    }

    // =========================================================================
    // getAccount Tests
    // =========================================================================

    @Test
    @DisplayName("Should get account by account number")
    void shouldGetAccountByAccountNumber() {
        // Given
        Account createdAccount = accountService.createMainAccount(testCustomer);

        // When
        Account retrievedAccount = accountService.getAccount(createdAccount.getAccountNumber());

        // Then
        assertThat(retrievedAccount).isNotNull();
        assertThat(retrievedAccount.getAccountNumber()).isEqualTo(createdAccount.getAccountNumber());
        assertThat(retrievedAccount.getBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(retrievedAccount.getCustomer().getCustomerId()).isEqualTo(testCustomer.getCustomerId());
    }

    @Test
    @DisplayName("Should throw exception when account not found")
    void shouldThrowExceptionWhenAccountNotFound() {
        // Given - Non-existent account number (valid Luhn)
        Long nonExistentAccountNumber = 9876543217L;

        // When & Then
        assertThatThrownBy(() -> accountService.getAccount(nonExistentAccountNumber))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Account not found");
    }

    @Test
    @DisplayName("Should throw exception when getting account of deleted customer")
    void shouldThrowExceptionWhenGettingAccountOfDeletedCustomer() {
        // Given - Create account then soft-delete customer
        Account account = accountService.createMainAccount(testCustomer);

        testCustomer.setActive(false);
        customerRepository.save(testCustomer);

        // When & Then - Should reject access (customer is inactive)
        assertThatThrownBy(() -> accountService.getAccount(account.getAccountNumber()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Account not found");
    }

    // =========================================================================
    // getCustomerAccounts Tests
    // =========================================================================

    @Test
    @DisplayName("Should get all accounts for customer")
    void shouldGetAllAccountsForCustomer() {
        // Given
        Account mainAccount = accountService.createMainAccount(testCustomer);

        // When
        List<Account> accounts = accountService.getCustomerAccounts(testCustomer.getCustomerId());

        // Then
        assertThat(accounts).hasSize(1);
        assertThat(accounts.getFirst().getAccountNumber()).isEqualTo(mainAccount.getAccountNumber());
        assertThat(accounts.getFirst().isMainAccount()).isTrue();
    }

    @Test
    @DisplayName("Should return empty list when customer has no accounts")
    void shouldReturnEmptyListWhenCustomerHasNoAccounts() {
        // Given - Customer with no accounts
        Customer customerWithoutAccount = new Customer();
        customerWithoutAccount.setCustomerId(99999997L);
        customerWithoutAccount.setFirstName("NoAccount");
        customerWithoutAccount.setLastName("User");
        customerWithoutAccount.setEmail("noaccoun@example.com");
        customerWithoutAccount.setMobileNumber("+27821111111");
        customerWithoutAccount = customerRepository.save(customerWithoutAccount);

        // When
        List<Account> accounts = accountService.getCustomerAccounts(customerWithoutAccount.getCustomerId());

        // Then
        assertThat(accounts).isEmpty();
    }

    @Test
    @DisplayName("Should not return accounts of soft-deleted customer")
    void shouldNotReturnAccountsOfSoftDeletedCustomer() {
        // Given - Create account then soft-delete customer
        accountService.createMainAccount(testCustomer);

        testCustomer.setActive(false);
        customerRepository.save(testCustomer);

        // When
        List<Account> accounts = accountService.getCustomerAccounts(testCustomer.getCustomerId());

        // Then - Should return empty list (customer is inactive, filtered by @SQLRestriction)
        assertThat(accounts).isEmpty();
    }

    // =========================================================================
    // updateBalance Tests
    // =========================================================================

    @Test
    @DisplayName("Should update account balance atomically")
    void shouldUpdateAccountBalanceAtomically() {
        // Given
        Account account = accountService.createMainAccount(testCustomer);
        assertThat(account.getBalance()).isEqualByComparingTo(BigDecimal.ZERO);

        // When - Update balance
        accountService.updateBalance(account, new BigDecimal("100.00"));

        // Then - Verify balance was updated in database
        Account updatedAccount = accountRepository.findById(account.getAccountNumber())
                .orElseThrow(() -> new AssertionError(
                        "Expected account " + account.getAccountNumber() + " to exist after balance update"
                ));
        assertThat(updatedAccount.getBalance()).isEqualByComparingTo(new BigDecimal("100.00"));
    }

    @Test
    @DisplayName("Should handle multiple sequential balance updates")
    void shouldHandleMultipleSequentialBalanceUpdates() {
        // Given
        Account account = accountService.createMainAccount(testCustomer);

        // When - Multiple updates
        accountService.updateBalance(account, new BigDecimal("50.00"));
        accountService.updateBalance(account, new BigDecimal("25.00"));
        accountService.updateBalance(account, new BigDecimal("12.50"));

        // Then - Balance should accumulate
        Account updatedAccount = accountRepository.findById(account.getAccountNumber())
                .orElseThrow(() -> new AssertionError(
                        "Expected account " + account.getAccountNumber() + " to exist after multiple updates"
                ));
        assertThat(updatedAccount.getBalance()).isEqualByComparingTo(new BigDecimal("87.50"));
    }

    @Test
    @DisplayName("Should update updatedAt timestamp when balance changes")
    void shouldUpdateUpdatedAtTimestampWhenBalanceChanges() throws InterruptedException {
        // Given
        Account account = accountService.createMainAccount(testCustomer);

        Account initialAccount = accountRepository.findById(account.getAccountNumber())
                .orElseThrow(() -> new AssertionError(
                        "Expected account " + account.getAccountNumber() + " to exist"
                ));
        assertThat(initialAccount.getCreatedAt()).isNotNull();
        assertThat(initialAccount.getUpdatedAt()).isNotNull();
        assertThat(initialAccount.getUpdatedAt()).isEqualTo(initialAccount.getCreatedAt());

        // Small delay to ensure timestamp difference
        Thread.sleep(10);

        // When - Update balance
        accountService.updateBalance(account, new BigDecimal("100.00"));

        // Then - updatedAt should change, createdAt should not
        Account updatedAccount = accountRepository.findById(account.getAccountNumber())
                .orElseThrow(() -> new AssertionError(
                        "Expected account " + account.getAccountNumber() + " to exist after update"
                ));

        assertThat(updatedAccount.getCreatedAt())
                .as("createdAt should not change")
                .isEqualTo(initialAccount.getCreatedAt());

        assertThat(updatedAccount.getUpdatedAt())
                .as("updatedAt should be after initial timestamp")
                .isAfter(initialAccount.getUpdatedAt());
    }

    @Test
    @DisplayName("Should throw exception when updating balance of non-existent account")
    void shouldThrowExceptionWhenUpdatingBalanceOfNonExistentAccount() {
        // Given - Account entity that doesn't exist in DB
        Account nonExistentAccount = new Account();
        nonExistentAccount.setAccountNumber(9876543217L); // Valid Luhn but not in DB

        // When & Then
        assertThatThrownBy(() -> accountService.updateBalance(nonExistentAccount, new BigDecimal("100.00")))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Unable to update account balance");
    }
}