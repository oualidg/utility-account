/*
 * Copyright (c) 2026 Oualid Gharach. All rights reserved.
 *
 * Aiming for production-grade standards through clean code and best practices
 * for educational and informational purposes.
 *
 * Created on: 3/1/2026 at 4:21 PM
 *
 * Feel free to use or contribute. Contact: oualid.gharach@gmail.com
 */
package com.mycompany.api.account.service;

import com.mycompany.api.account.BaseIntegrationTest;
import com.mycompany.api.account.dto.CreateCustomerRequest;
import com.mycompany.api.account.dto.DepositRequest;
import com.mycompany.api.account.entity.Account;
import com.mycompany.api.account.entity.Customer;
import com.mycompany.api.account.entity.PaymentProvider;
import com.mycompany.api.account.exception.BalanceUpdateException;
import com.mycompany.api.account.repository.AccountRepository;
import com.mycompany.api.account.repository.CustomerRepository;
import com.mycompany.api.account.repository.PaymentProviderRepository;
import com.mycompany.api.account.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

/**
 * Explicit transaction rollback tests.
 *
 * <p>Verifies atomicity under failure for the two transaction helpers.
 * Uses {@code @MockitoSpyBean} to inject failures at precise points while
 * preserving the real Spring {@code @Transactional} proxy wiring.</p>
 *
 * <p>Kept in its own class because {@code @MockitoSpyBean} taints the
 * application context and must not be mixed with other integration tests.</p>
 *
 * @author Oualid Gharach
 */
@DisplayName("Transaction Rollback Tests")
class TransactionRollbackTest extends BaseIntegrationTest {

    @Autowired
    private PaymentTransactionHelper paymentTransactionHelper;

    @Autowired
    private CustomerTransactionHelper customerTransactionHelper;

    // Spy preserves the real bean (and its @Transactional proxy) while
    // allowing us to stub individual methods to simulate mid-transaction failures.
    @MockitoSpyBean
    private AccountService accountService;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private PaymentProviderRepository providerRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Account testAccount;
    private PaymentProvider mpesaProvider;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("DELETE FROM payments");
        jdbcTemplate.execute("DELETE FROM accounts");
        jdbcTemplate.execute("DELETE FROM customers");
        jdbcTemplate.execute("DELETE FROM payment_providers");

        mpesaProvider = new PaymentProvider();
        mpesaProvider.setCode("MPESA");
        mpesaProvider.setName("M-Pesa");
        mpesaProvider.setApiKeyHash("mpesa-test-hash");
        mpesaProvider.setApiKeyPrefix("mpesa123");
        mpesaProvider.setActive(true);
        mpesaProvider = providerRepository.save(mpesaProvider);

        Customer testCustomer = new Customer();
        testCustomer.setCustomerId(12345674L);
        testCustomer.setFirstName("John");
        testCustomer.setLastName("Doe");
        testCustomer.setEmail("john@example.com");
        testCustomer.setMobileNumber("+27821234567");
        testCustomer = customerRepository.save(testCustomer);

        testAccount = new Account();
        testAccount.setAccountNumber(1234567897L);
        testAccount.setCustomer(testCustomer);
        testAccount.setBalance(BigDecimal.ZERO);
        testAccount.setMainAccount(true);
        testAccount = accountRepository.save(testAccount);
    }

    @Test
    @DisplayName("Should roll back payment INSERT when balance update fails — no phantom credit, no orphan payment row")
    void shouldRollbackPaymentInsertWhenBalanceUpdateFails() {
        // Given — payment INSERT will succeed, but balance update will fail
        doThrow(new BalanceUpdateException("Simulated DB constraint failure"))
                .when(accountService).updateBalance(any(Account.class), any(BigDecimal.class));

        DepositRequest request = new DepositRequest(
                new BigDecimal("100.00"),
                "MPESA-REF-ROLLBACK"
        );

        // When — the whole transaction must roll back
        assertThatThrownBy(() ->
                paymentTransactionHelper.executeDeposit(
                        request, mpesaProvider,
                        () -> accountService.getAccount(testAccount.getAccountNumber()),
                        "rollback-test"
                ))
                .isInstanceOf(BalanceUpdateException.class);

        // Then — payment INSERT was rolled back, no orphan row
        assertThat(paymentRepository.count())
                .as("Payment row must not be committed when balance update fails")
                .isZero();

        // Then — balance unchanged, no phantom credit
        Account unchanged = accountRepository.findById(testAccount.getAccountNumber()).orElseThrow();
        assertThat(unchanged.getBalance())
                .as("Balance must remain zero when the enclosing transaction rolls back")
                .isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Should roll back customer INSERT when account creation fails — no orphan customer row")
    void shouldRollbackCustomerInsertWhenAccountCreationFails() {
        // Clean slate — outer setUp seeds a customer + account we don't want here
        jdbcTemplate.execute("DELETE FROM accounts");
        jdbcTemplate.execute("DELETE FROM customers");

        // Given — customer saveAndFlush will succeed, but createMainAccount will fail
        doThrow(new RuntimeException("Simulated account creation failure"))
                .when(accountService).createMainAccount(any(Customer.class));

        CreateCustomerRequest request = new CreateCustomerRequest(
                "Jane", "Smith", "jane@example.com", "+27829876543"
        );

        // When — the whole transaction must roll back
        assertThatThrownBy(() -> customerTransactionHelper.executeCreateCustomer(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Simulated account creation failure");

        // Then — no orphan customer row left behind
        assertThat(customerRepository.count())
                .as("Customer row must be rolled back when account creation fails — no orphans allowed")
                .isZero();
    }
}