/*
 * Copyright (c) 2026 Oualid Gharach. All rights reserved.
 *
 * Aiming for production-grade standards through clean code and best practices
 * for educational and informational purposes.
 *
 * Created on: 2/11/2026 at 10:10 PM
 *
 * Feel free to use or contribute. Contact: oualid.gharach@gmail.com
 */
package com.mycompany.api.account.controller;

import com.mycompany.api.account.dto.AccountResponse;
import com.mycompany.api.account.dto.AccountSummaryResponse;
import com.mycompany.api.account.entity.Account;
import com.mycompany.api.account.entity.Customer;
import com.mycompany.api.account.exception.ResourceNotFoundException;
import com.mycompany.api.account.mapper.AccountMapper;
import com.mycompany.api.account.service.AccountService;
import com.mycompany.api.account.service.ProviderService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for AccountController.
 * Uses @WebMvcTest to test only the web layer with mocked service.
 *
 * @author Oualid Gharach
 */
@WebMvcTest(AccountController.class)
@TestPropertySource(properties = "spring.main.banner-mode=off")
@DisplayName("AccountController Unit Tests")
class AccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AccountService accountService;

    @MockitoBean
    private AccountMapper accountMapper;

    @MockitoBean
    private ProviderService providerService;

    // Real mapper instance for creating test responses
    private final AccountMapper realMapper = Mappers.getMapper(AccountMapper.class);

    // =========================================================================
    // GET /api/v1/customers/{customerId}/accounts - Get Customer Accounts
    // =========================================================================

    @Test
    @DisplayName("Should get all accounts for customer successfully")
    void shouldGetAllAccountsForCustomer() throws Exception {
        // Given
        Long customerId = 12345674L;  // Valid Luhn

        Account account1 = createAccount(1234567897L, customerId, new BigDecimal("100.00"), true);
        Account account2 = createAccount(9876543217L, customerId, new BigDecimal("50.00"), false);

        List<Account> accounts = List.of(account1, account2);

        AccountSummaryResponse response1 = realMapper.toSummaryResponse(account1);
        AccountSummaryResponse response2 = realMapper.toSummaryResponse(account2);

        when(accountService.getCustomerAccounts(customerId)).thenReturn(accounts);
        when(accountMapper.toSummaryResponse(account1)).thenReturn(response1);
        when(accountMapper.toSummaryResponse(account2)).thenReturn(response2);

        // When & Then
        mockMvc.perform(get("/api/v1/customers/{customerId}/accounts", customerId))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].accountNumber").value(1234567897L))
                .andExpect(jsonPath("$[0].balance", is(100.00)))
                .andExpect(jsonPath("$[0].isMainAccount", is(true)))
                .andExpect(jsonPath("$[1].accountNumber").value(9876543217L))
                .andExpect(jsonPath("$[1].balance", is(50.00)))
                .andExpect(jsonPath("$[1].isMainAccount", is(false)));
    }

    @Test
    @DisplayName("Should return empty list when customer has no accounts")
    void shouldReturnEmptyListWhenCustomerHasNoAccounts() throws Exception {
        // Given
        Long customerId = 12345674L;  // Valid Luhn

        when(accountService.getCustomerAccounts(customerId)).thenReturn(List.of());

        // When & Then
        mockMvc.perform(get("/api/v1/customers/{customerId}/accounts", customerId))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("Should return 400 when customer ID fails Luhn validation")
    void shouldReturn400WhenCustomerIdFailsLuhnValidation() throws Exception {
        // Given - Invalid Luhn customer ID
        Long invalidCustomerId = 12345671L;  // Invalid checksum

        // When & Then
        mockMvc.perform(get("/api/v1/customers/{customerId}/accounts", invalidCustomerId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Customer ID must be a valid 8-digit number with checksum")));
    }

    @Test
    @DisplayName("Should return 404 when customer not found")
    void shouldReturn404WhenCustomerNotFound() throws Exception {
        // Given - Valid Luhn but non-existent customer
        Long customerId = 99999997L;  // Valid Luhn

        when(accountService.getCustomerAccounts(customerId))
                .thenThrow(new ResourceNotFoundException("Customer not found with ID: " + customerId));

        // When & Then
        mockMvc.perform(get("/api/v1/customers/{customerId}/accounts", customerId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", is("Customer not found with ID: " + customerId)));
    }

    // =========================================================================
    // GET /api/v1/accounts/{accountNumber} - Get Account by Number
    // =========================================================================

    @Test
    @DisplayName("Should get account by account number successfully")
    void shouldGetAccountByAccountNumber() throws Exception {
        // Given
        Long accountNumber = 1234567897L;  // Valid Luhn
        Long customerId = 12345674L;

        Account account = createAccount(accountNumber, customerId, new BigDecimal("250.75"), true);
        AccountResponse response = realMapper.toResponse(account);

        when(accountService.getAccount(accountNumber)).thenReturn(account);
        when(accountMapper.toResponse(account)).thenReturn(response);

        // When & Then
        mockMvc.perform(get("/api/v1/accounts/{accountNumber}", accountNumber))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.accountNumber").value(1234567897L))
                .andExpect(jsonPath("$.customerId").value(12345674L))
                .andExpect(jsonPath("$.balance", is(250.75)))
                .andExpect(jsonPath("$.isMainAccount", is(true)));
    }

    @Test
    @DisplayName("Should return 400 when account number fails Luhn validation")
    void shouldReturn400WhenAccountNumberFailsLuhnValidation() throws Exception {
        // Given - Invalid Luhn account number
        Long invalidAccountNumber = 1234567891L;  // Invalid checksum

        // When & Then
        mockMvc.perform(get("/api/v1/accounts/{accountNumber}", invalidAccountNumber))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Account number must be a valid 10-digit number with checksum")));
    }

    @Test
    @DisplayName("Should return 404 when account not found")
    void shouldReturn404WhenAccountNotFound() throws Exception {
        // Given - Valid Luhn but non-existent account
        Long accountNumber = 9876543217L;  // Valid Luhn

        when(accountService.getAccount(accountNumber))
                .thenThrow(new ResourceNotFoundException("Account not found with account number: " + accountNumber));

        // When & Then
        mockMvc.perform(get("/api/v1/accounts/{accountNumber}", accountNumber))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", is("Account not found with account number: " + accountNumber)));
    }

    // =========================================================================
    // Helper Methods
    // =========================================================================

    private Account createAccount(Long accountNumber, Long customerId, BigDecimal balance, boolean isMainAccount) {
        Customer customer = new Customer();
        customer.setCustomerId(customerId);
        customer.setFirstName("John");
        customer.setLastName("Doe");
        customer.setEmail("john@example.com");
        customer.setMobileNumber("+27821234567");

        Account account = new Account();
        account.setAccountNumber(accountNumber);
        account.setCustomer(customer);
        account.setBalance(balance);
        account.setMainAccount(isMainAccount);
        account.setCreatedAt(Instant.parse("2026-02-11T10:00:00Z"));
        account.setUpdatedAt(Instant.parse("2026-02-11T15:30:00Z"));

        return account;
    }
}