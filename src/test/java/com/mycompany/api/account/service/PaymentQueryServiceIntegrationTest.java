/*
 * Copyright (c) 2026 Oualid Gharach. All rights reserved.
 *
 * Aiming for production-grade standards through clean code and best practices
 * for educational and informational purposes.
 *
 * Created on: 2/20/2026 at 7:14 PM
 *
 * Feel free to use or contribute. Contact: oualid.gharach@gmail.com
 */
package com.mycompany.api.account.service;

import com.mycompany.api.account.BaseIntegrationTest;
import com.mycompany.api.account.dto.PaymentResponse;
import com.mycompany.api.account.dto.PaymentSummaryResponse;
import com.mycompany.api.account.dto.ProviderReconciliationResponse;
import com.mycompany.api.account.entity.Account;
import com.mycompany.api.account.entity.Customer;
import com.mycompany.api.account.entity.Payment;
import com.mycompany.api.account.entity.PaymentProvider;
import com.mycompany.api.account.exception.ResourceNotFoundException;
import com.mycompany.api.account.repository.AccountRepository;
import com.mycompany.api.account.repository.CustomerRepository;
import com.mycompany.api.account.repository.PaymentProviderRepository;
import com.mycompany.api.account.repository.PaymentRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("PaymentQueryService Integration Tests")
class PaymentQueryServiceIntegrationTest extends BaseIntegrationTest {

    @Autowired private PaymentQueryService paymentQueryService;
    @Autowired private PaymentRepository paymentRepository;
    @Autowired private AccountRepository accountRepository;
    @Autowired private CustomerRepository customerRepository;
    @Autowired private PaymentProviderRepository providerRepository;
    @Autowired private JdbcTemplate jdbcTemplate;

    private PaymentProvider mpesa;
    private PaymentProvider mtn;
    private Account account1;
    private Account account2;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("DELETE FROM payments");
        jdbcTemplate.execute("DELETE FROM accounts");
        jdbcTemplate.execute("DELETE FROM customers");
        jdbcTemplate.execute("DELETE FROM payment_providers");

        mpesa = seedProvider("MPESA", "M-Pesa", "mpesa-test-hash", "mpesa123");
        mtn   = seedProvider("MTN", "MTN Mobile Money", "mtn-test-hash", "mtn12345");

        Customer customer1 = saveCustomer(12345674L, "alice@test.com", "+27821000001");
        Customer customer2 = saveCustomer(98765438L, "bob@test.com",   "+27821000002");

        account1 = saveAccount(1234567897L, customer1);
        account2 = saveAccount(9876543217L, customer2);
    }

    // -------------------------------------------------------------------------
    // searchPayments
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Should return all payments when no filters provided")
    void shouldReturnAllPaymentsWithNoFilters() {
        savePayment(account1, mpesa, "REF-001", new BigDecimal("100.00"), Instant.now());
        savePayment(account2, mtn,   "REF-002", new BigDecimal("200.00"), Instant.now());

        List<PaymentResponse> results = paymentQueryService.searchPayments(null, null, null, null, null);

        assertThat(results).hasSize(2);
    }

    @Test
    @DisplayName("Should filter payments by account number")
    void shouldFilterByAccountNumber() {
        savePayment(account1, mpesa, "REF-001", new BigDecimal("100.00"), Instant.now());
        savePayment(account2, mtn,   "REF-002", new BigDecimal("200.00"), Instant.now());

        List<PaymentResponse> results = paymentQueryService
                .searchPayments(account1.getAccountNumber(), null, null, null, null);

        assertThat(results).hasSize(1);
        assertThat(results.getFirst().accountNumber()).isEqualTo(account1.getAccountNumber());
    }

    @Test
    @DisplayName("Should filter payments by customer ID")
    void shouldFilterByCustomerId() {
        savePayment(account1, mpesa, "REF-001", new BigDecimal("100.00"), Instant.now());
        savePayment(account2, mtn,   "REF-002", new BigDecimal("200.00"), Instant.now());

        List<PaymentResponse> results = paymentQueryService
                .searchPayments(null, account1.getCustomer().getCustomerId(), null, null, null);

        assertThat(results).hasSize(1);
        assertThat(results.getFirst().accountNumber()).isEqualTo(account1.getAccountNumber());
    }

    @Test
    @DisplayName("Should filter payments by provider code")
    void shouldFilterByProviderCode() {
        savePayment(account1, mpesa, "REF-001", new BigDecimal("100.00"), Instant.now());
        savePayment(account2, mtn,   "REF-002", new BigDecimal("200.00"), Instant.now());

        List<PaymentResponse> results = paymentQueryService
                .searchPayments(null, null, "MPESA", null, null);

        assertThat(results).hasSize(1);
        assertThat(results.getFirst().providerCode()).isEqualTo("MPESA");
    }

    @Test
    @DisplayName("Should filter payments by date range")
    void shouldFilterByDateRange() {
        Instant yesterday = Instant.now().minus(1, ChronoUnit.DAYS);
        Instant twoDaysAgo = Instant.now().minus(2, ChronoUnit.DAYS);

        savePayment(account1, mpesa, "REF-001", new BigDecimal("100.00"), Instant.now());
        savePayment(account2, mtn,   "REF-002", new BigDecimal("200.00"), twoDaysAgo);

        List<PaymentResponse> results = paymentQueryService
                .searchPayments(null, null, null, yesterday, null);

        assertThat(results).hasSize(1);
        assertThat(results.getFirst().paymentReference()).isEqualTo("REF-001");
    }

    @Test
    @DisplayName("Should return empty list when no payments match filters")
    void shouldReturnEmptyListWhenNoMatch() {
        List<PaymentResponse> results = paymentQueryService
                .searchPayments(null, null, "MPESA", null, null);

        assertThat(results).isEmpty();
    }

    // -------------------------------------------------------------------------
    // getDashboardSummary
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Should return correct global totals")
    void shouldReturnCorrectGlobalTotals() {
        savePayment(account1, mpesa, "REF-001", new BigDecimal("100.00"), Instant.now());
        savePayment(account1, mpesa, "REF-002", new BigDecimal("150.00"), Instant.now());
        savePayment(account2, mtn,   "REF-003", new BigDecimal("200.00"), Instant.now());

        PaymentSummaryResponse summary = paymentQueryService.getDashboardSummary(null, null);

        assertThat(summary.totalAmount()).isEqualByComparingTo(new BigDecimal("450.00"));
        assertThat(summary.totalCount()).isEqualTo(3L);
        assertThat(summary.byProvider()).hasSize(2);
    }

    @Test
    @DisplayName("Should return zero totals when no payments exist")
    void shouldReturnZeroTotalsWhenNoPayments() {
        PaymentSummaryResponse summary = paymentQueryService.getDashboardSummary(null, null);

        assertThat(summary.totalAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(summary.totalCount()).isZero();
        assertThat(summary.byProvider()).isEmpty();
    }

    @Test
    @DisplayName("Should scope dashboard summary to date range")
    void shouldScopeSummaryToDateRange() {
        Instant yesterday = Instant.now().minus(1, ChronoUnit.DAYS);

        savePayment(account1, mpesa, "REF-001", new BigDecimal("100.00"), Instant.now());
        savePayment(account2, mtn,   "REF-002", new BigDecimal("200.00"),
                Instant.now().minus(2, ChronoUnit.DAYS));

        PaymentSummaryResponse summary = paymentQueryService.getDashboardSummary(yesterday, null);

        assertThat(summary.totalAmount()).isEqualByComparingTo(new BigDecimal("100.00"));
        assertThat(summary.totalCount()).isEqualTo(1L);
    }

    // -------------------------------------------------------------------------
    // getAccountSummary
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Should return correct account totals")
    void shouldReturnCorrectAccountTotals() {
        savePayment(account1, mpesa, "REF-001", new BigDecimal("100.00"), Instant.now());
        savePayment(account1, mpesa, "REF-002", new BigDecimal("50.00"),  Instant.now());
        savePayment(account2, mtn,   "REF-003", new BigDecimal("200.00"), Instant.now());

        PaymentSummaryResponse summary = paymentQueryService
                .getAccountSummary(account1.getAccountNumber());

        assertThat(summary.totalAmount()).isEqualByComparingTo(new BigDecimal("150.00"));
        assertThat(summary.totalCount()).isEqualTo(2L);
        assertThat(summary.byProvider()).hasSize(1);
    }

    @Test
    @DisplayName("Should return zero totals for account with no payments")
    void shouldReturnZeroForAccountWithNoPayments() {
        PaymentSummaryResponse summary = paymentQueryService
                .getAccountSummary(account1.getAccountNumber());

        assertThat(summary.totalAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(summary.totalCount()).isZero();
    }

    // -------------------------------------------------------------------------
    // getReconciliation
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Should return reconciliation report for provider")
    void shouldReturnReconciliationReport() {
        savePayment(account1, mpesa, "REF-001", new BigDecimal("100.00"), Instant.now());
        savePayment(account1, mpesa, "REF-002", new BigDecimal("150.00"), Instant.now());
        savePayment(account2, mtn,   "REF-003", new BigDecimal("200.00"), Instant.now());

        ProviderReconciliationResponse report = paymentQueryService
                .getReconciliation("MPESA", null, null);

        assertThat(report.providerCode()).isEqualTo("MPESA");
        assertThat(report.providerName()).isEqualTo("M-Pesa");
        assertThat(report.totalAmount()).isEqualByComparingTo(new BigDecimal("250.00"));
        assertThat(report.totalCount()).isEqualTo(2L);
        assertThat(report.payments()).hasSize(2);
    }

    @Test
    @DisplayName("Should return empty reconciliation when no payments in range")
    void shouldReturnEmptyReconciliationWhenNoPayments() {
        Instant tomorrow = Instant.now().plus(1, ChronoUnit.DAYS);

        ProviderReconciliationResponse report = paymentQueryService
                .getReconciliation("MPESA", tomorrow, null);

        assertThat(report.totalAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(report.totalCount()).isZero();
        assertThat(report.payments()).isEmpty();
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException for unknown provider")
    void shouldThrowForUnknownProvider() {
        assertThatThrownBy(() -> paymentQueryService.getReconciliation("UNKNOWN", null, null))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Provider not found: UNKNOWN");
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private Customer saveCustomer(Long id, String email, String mobile) {
        Customer c = new Customer();
        c.setCustomerId(id);
        c.setFirstName("Test");
        c.setLastName("User");
        c.setEmail(email);
        c.setMobileNumber(mobile);
        c.setActive(true);
        return customerRepository.save(c);
    }

    private Account saveAccount(Long accountNumber, Customer customer) {
        Account a = new Account();
        a.setAccountNumber(accountNumber);
        a.setCustomer(customer);
        a.setBalance(BigDecimal.ZERO);
        a.setMainAccount(true);
        return accountRepository.save(a);
    }

    private void savePayment(Account account, PaymentProvider provider,
                             String reference, BigDecimal amount, Instant date) {
        Payment p = new Payment();
        p.setReceiptNumber(java.util.UUID.randomUUID().toString());
        p.setAccount(account);
        p.setPaymentProvider(provider);
        p.setPaymentReference(reference);
        p.setAmount(amount);
        p.setPaymentDate(date);
        paymentRepository.save(p);
        // Override the date set by @PrePersist
        jdbcTemplate.update(
                "UPDATE payments SET payment_date = ? WHERE receipt_number = ?",
                java.sql.Timestamp.from(date), p.getReceiptNumber()
        );
    }

    private PaymentProvider seedProvider(String code, String name, String hash, String prefix) {
        PaymentProvider p = new PaymentProvider();
        p.setCode(code);
        p.setName(name);
        p.setApiKeyHash(hash);
        p.setApiKeyPrefix(prefix);
        p.setActive(true);
        return providerRepository.save(p);
    }
}