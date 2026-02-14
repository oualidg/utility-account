/*
 * Copyright (c) 2026 Oualid Gharach. All rights reserved.
 *
 * Aiming for production-grade standards through clean code and best practices
 * for educational and informational purposes.
 *
 * Created on: 2/11/2026 at 7:49 PM
 *
 * Feel free to use or contribute. Contact: oualid.gharach@gmail.com
 */
package com.mycompany.api.account.mapper;

import com.mycompany.api.account.dto.AccountResponse;
import com.mycompany.api.account.dto.AccountSummaryResponse;
import com.mycompany.api.account.entity.Account;
import com.mycompany.api.account.entity.Customer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for AccountMapper.
 * Tests MapStruct mapping between Account entity and Account DTOs.
 *
 * @author Oualid Gharach
 */
@DisplayName("AccountMapper Unit Tests")
class AccountMapperTest {

    private static final Instant TEST_CREATED_AT = Instant.parse("2026-02-11T10:00:00Z");
    private static final Instant TEST_UPDATED_AT = Instant.parse("2026-02-11T15:30:00Z");

    private AccountMapper accountMapper;

    @BeforeEach
    void setUp() {
        // Get MapStruct generated implementation
        accountMapper = Mappers.getMapper(AccountMapper.class);
    }

    @Test
    @DisplayName("Should map Account entity to AccountSummaryResponse with all fields")
    void shouldMapAccountToSummaryResponse() {
        // Given
        Account account = createBasicAccount();

        // When
        AccountSummaryResponse response = accountMapper.toSummaryResponse(account);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.accountNumber()).isEqualTo(1234567890L);
        assertThat(response.balance()).isEqualByComparingTo(new BigDecimal("100.50"));
        assertThat(response.isMainAccount()).isTrue();
        assertThat(response.createdAt()).isEqualTo(TEST_CREATED_AT);
    }

    @Test
    @DisplayName("Should map Account entity to AccountResponse with all fields including customerId")
    void shouldMapAccountToFullResponse() {
        // Given
        Account account = createBasicAccount();

        // When
        AccountResponse response = accountMapper.toResponse(account);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.accountNumber()).isEqualTo(1234567890L);
        assertThat(response.customerId())
                .as("Should extract customerId from nested customer entity")
                .isEqualTo(12345670L);
        assertThat(response.balance()).isEqualByComparingTo(new BigDecimal("100.50"));
        assertThat(response.isMainAccount()).isTrue();
        assertThat(response.createdAt()).isEqualTo(TEST_CREATED_AT);
        assertThat(response.updatedAt()).isEqualTo(TEST_UPDATED_AT);
    }

    @Test
    @DisplayName("Should preserve decimal precision for balance")
    void shouldPreserveDecimalPrecision() {
        // Given - Test various decimal balances
        BigDecimal[] testBalances = {
                BigDecimal.ZERO,             // Zero balance
                new BigDecimal("0.01"),      // Minimum
                new BigDecimal("100.00"),    // Whole number
                new BigDecimal("123.45"),    // Standard decimal
                new BigDecimal("999.99"),    // Near maximum
                new BigDecimal("9999999.99") // Large balance
        };

        for (BigDecimal testBalance : testBalances) {
            Account account = createAccountWithBalance(testBalance);

            // When
            AccountSummaryResponse summaryResponse = accountMapper.toSummaryResponse(account);
            AccountResponse fullResponse = accountMapper.toResponse(account);

            // Then
            assertThat(summaryResponse.balance())
                    .as("Summary response balance %s should be preserved exactly", testBalance)
                    .isEqualByComparingTo(testBalance);

            assertThat(fullResponse.balance())
                    .as("Full response balance %s should be preserved exactly", testBalance)
                    .isEqualByComparingTo(testBalance);
        }
    }

    @Test
    @DisplayName("Should map main account and sub-account correctly")
    void shouldMapIsMainAccountFlag() {
        // Test main account
        Account mainAccount = createBasicAccount();
        mainAccount.setMainAccount(true);

        AccountSummaryResponse mainSummary = accountMapper.toSummaryResponse(mainAccount);
        AccountResponse mainFull = accountMapper.toResponse(mainAccount);

        assertThat(mainSummary.isMainAccount()).isTrue();
        assertThat(mainFull.isMainAccount()).isTrue();

        // Test sub-account
        Account subAccount = createBasicAccount();
        subAccount.setMainAccount(false);

        AccountSummaryResponse subSummary = accountMapper.toSummaryResponse(subAccount);
        AccountResponse subFull = accountMapper.toResponse(subAccount);

        assertThat(subSummary.isMainAccount()).isFalse();
        assertThat(subFull.isMainAccount()).isFalse();
    }

    @Test
    @DisplayName("Should handle null account gracefully")
    void shouldHandleNullAccount() {
        // When
        AccountSummaryResponse summaryResponse = accountMapper.toSummaryResponse(null);
        AccountResponse fullResponse = accountMapper.toResponse(null);

        // Then
        assertThat(summaryResponse).isNull();
        assertThat(fullResponse).isNull();
    }

    // =========================================================================
    // Helper Methods
    // =========================================================================

    private Account createBasicAccount() {
        Customer customer = new Customer();
        customer.setCustomerId(12345670L);
        customer.setFirstName("John");
        customer.setLastName("Doe");
        customer.setEmail("john@example.com");
        customer.setMobileNumber("+27821234567");

        Account account = new Account();
        account.setAccountNumber(1234567890L);
        account.setCustomer(customer);
        account.setBalance(new BigDecimal("100.50"));
        account.setMainAccount(true);
        account.setCreatedAt(TEST_CREATED_AT);
        account.setUpdatedAt(TEST_UPDATED_AT);

        return account;
    }

    private Account createAccountWithBalance(BigDecimal balance) {
        Account account = createBasicAccount();
        account.setBalance(balance);
        return account;
    }
}