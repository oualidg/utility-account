/*
 * Copyright (c) 2026 Oualid Gharach. All rights reserved.
 *
 * Aiming for production-grade standards through clean code and best practices
 * for educational and informational purposes.
 *
 * Created on: 2/13/2026 at 4:55 PM
 *
 * Feel free to use or contribute. Contact: oualid.gharach@gmail.com
 */
package com.mycompany.api.account.service;

import com.mycompany.api.account.dto.DepositRequest;
import com.mycompany.api.account.entity.Account;
import com.mycompany.api.account.entity.Customer;
import com.mycompany.api.account.entity.Payment;
import com.mycompany.api.account.exception.DuplicateResourceException;
import com.mycompany.api.account.exception.ResourceNotFoundException;
import com.mycompany.api.account.model.PaymentProvider;
import com.mycompany.api.account.repository.AccountRepository;
import com.mycompany.api.account.repository.CustomerRepository;
import com.mycompany.api.account.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integration tests for PaymentService.
 * Tests service layer with real database and repositories.
 *
 * @author Oualid Gharach
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("PaymentService Integration Tests")
class PaymentServiceIntegrationTest {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Customer testCustomer;
    private Account testAccount;

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

        // Create test account
        testAccount = new Account();
        testAccount.setAccountNumber(1234567897L);
        testAccount.setCustomer(testCustomer);
        testAccount.setBalance(BigDecimal.ZERO);
        testAccount.setMainAccount(true);
        testAccount = accountRepository.save(testAccount);
    }

    // =========================================================================
    // depositToAccount Tests
    // =========================================================================

    @Test
    @DisplayName("Should deposit to account successfully")
    void shouldDepositToAccountSuccessfully() {
        // Given
        DepositRequest request = new DepositRequest(
                new BigDecimal("100.00"),
                PaymentProvider.MPESA,
                "MPESA-REF-12345"
        );

        // When
        Payment payment = paymentService.depositToAccount(testAccount.getAccountNumber(), request);

        // Then
        assertThat(payment).isNotNull();
        assertThat(payment.getReceiptNumber()).isNotNull();
        assertThat(payment.getAccount().getAccountNumber()).isEqualTo(testAccount.getAccountNumber());
        assertThat(payment.getAmount()).isEqualByComparingTo(new BigDecimal("100.00"));
        assertThat(payment.getPaymentProvider()).isEqualTo(PaymentProvider.MPESA);
        assertThat(payment.getPaymentReference()).isEqualTo("MPESA-REF-12345");
        assertThat(payment.getPaymentDate()).isNotNull();

        // Verify balance was updated
        Account updatedAccount = accountRepository.findById(testAccount.getAccountNumber())
                .orElseThrow(() -> new AssertionError(
                        "Expected account " + testAccount.getAccountNumber() + " to exist in database after deposit"
                ));
        assertThat(updatedAccount.getBalance()).isEqualByComparingTo(new BigDecimal("100.00"));

        // Verify payment was persisted
        Payment savedPayment = paymentRepository.findById(payment.getReceiptNumber())
                .orElseThrow(() -> new AssertionError(
                        "Expected payment " + payment.getReceiptNumber() + " to be persisted in database"
                ));
        assertThat(savedPayment.getAmount()).isEqualByComparingTo(new BigDecimal("100.00"));
    }

    @Test
    @DisplayName("Should return existing payment when duplicate reference with same details (legitimate retry)")
    void shouldReturnExistingPaymentWhenLegitimateRetry() {
        // Given - First payment
        DepositRequest request1 = new DepositRequest(
                new BigDecimal("50.00"),
                PaymentProvider.MPESA,
                "MPESA-REF-DUPLICATE"
        );
        Payment payment1 = paymentService.depositToAccount(testAccount.getAccountNumber(), request1);

        // Get balance after first payment
        Account accountAfterFirst = accountRepository.findById(testAccount.getAccountNumber())
                .orElseThrow(() -> new AssertionError(
                        "Expected account " + testAccount.getAccountNumber() + " to exist after first payment"
                ));
        BigDecimal balanceAfterFirst = accountAfterFirst.getBalance();

        // When - Second payment with SAME reference AND same amount (legitimate retry)
        DepositRequest request2 = new DepositRequest(
                new BigDecimal("50.00"),  // Same amount
                PaymentProvider.MPESA,
                "MPESA-REF-DUPLICATE"  // Same reference
        );
        Payment payment2 = paymentService.depositToAccount(testAccount.getAccountNumber(), request2);

        // Then - Should return the FIRST payment (idempotency)
        assertThat(payment2.getReceiptNumber()).isEqualTo(payment1.getReceiptNumber());
        assertThat(payment2.getAmount()).isEqualByComparingTo(new BigDecimal("50.00"));

        // Verify balance was NOT updated again
        Account accountAfterSecond = accountRepository.findById(testAccount.getAccountNumber())
                .orElseThrow(() -> new AssertionError(
                        "Expected account " + testAccount.getAccountNumber() + " to exist after second payment attempt"
                ));
        assertThat(accountAfterSecond.getBalance()).isEqualByComparingTo(balanceAfterFirst);

        // Verify only ONE payment exists
        long paymentCount = paymentRepository.findAll().stream()
                .filter(p -> p.getPaymentReference().equals("MPESA-REF-DUPLICATE"))
                .count();
        assertThat(paymentCount).isEqualTo(1);
    }

    @Test
    @DisplayName("Should reject duplicate reference with different amount")
    void shouldRejectDuplicateReferenceWithDifferentAmount() {
        // Given - First payment
        DepositRequest request1 = new DepositRequest(
                new BigDecimal("50.00"),
                PaymentProvider.MPESA,
                "MPESA-REF-MISMATCH-AMT"
        );
        paymentService.depositToAccount(testAccount.getAccountNumber(), request1);

        // When/Then - Second payment with same reference but different amount
        DepositRequest request2 = new DepositRequest(
                new BigDecimal("75.00"),  // Different amount!
                PaymentProvider.MPESA,
                "MPESA-REF-MISMATCH-AMT"  // Same reference
        );
        assertThatThrownBy(() -> paymentService.depositToAccount(testAccount.getAccountNumber(), request2))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("already exists with different details");
    }

    @Test
    @DisplayName("Should reject duplicate reference targeting different account")
    void shouldRejectDuplicateReferenceWithDifferentAccount() {
        // Given - First payment to testAccount
        DepositRequest request1 = new DepositRequest(
                new BigDecimal("50.00"),
                PaymentProvider.MPESA,
                "MPESA-REF-MISMATCH-ACC"
        );
        paymentService.depositToAccount(testAccount.getAccountNumber(), request1);

        // Create a second account for the same customer
        Account secondAccount = new Account();
        secondAccount.setAccountNumber(9876543209L);
        secondAccount.setCustomer(testCustomer);
        secondAccount.setBalance(BigDecimal.ZERO);
        secondAccount.setMainAccount(false);
        accountRepository.save(secondAccount);

        // When/Then - Same reference but targeting a different account
        DepositRequest request2 = new DepositRequest(
                new BigDecimal("50.00"),  // Same amount
                PaymentProvider.MPESA,
                "MPESA-REF-MISMATCH-ACC"  // Same reference
        );
        assertThatThrownBy(() -> paymentService.depositToAccount(secondAccount.getAccountNumber(), request2))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("already exists with different details");
    }

    @Test
    @DisplayName("Should allow same reference with different provider")
    void shouldAllowSameReferenceWithDifferentProvider() {
        // Given - Payment with MPESA
        DepositRequest request1 = new DepositRequest(
                new BigDecimal("50.00"),
                PaymentProvider.MPESA,
                "REF-12345"
        );
        Payment payment1 = paymentService.depositToAccount(testAccount.getAccountNumber(), request1);

        // When - Payment with MTN_MOMO (different provider, same reference)
        DepositRequest request2 = new DepositRequest(
                new BigDecimal("75.00"),
                PaymentProvider.MTN_MOMO,  // Different provider!
                "REF-12345"  // Same reference!
        );
        Payment payment2 = paymentService.depositToAccount(testAccount.getAccountNumber(), request2);

        // Then - Should create NEW payment (different composite key)
        assertThat(payment2.getReceiptNumber()).isNotEqualTo(payment1.getReceiptNumber());
        assertThat(payment2.getAmount()).isEqualByComparingTo(new BigDecimal("75.00"));

        // Verify balance was updated TWICE
        Account updatedAccount = accountRepository.findById(testAccount.getAccountNumber())
                .orElseThrow(() -> new AssertionError(
                        "Expected account " + testAccount.getAccountNumber() + " to exist after both payments"
                ));
        assertThat(updatedAccount.getBalance()).isEqualByComparingTo(new BigDecimal("125.00"));

        // Verify TWO payments exist
        assertThat(paymentRepository.count()).isEqualTo(2);
    }

    @Test
    @DisplayName("Should throw exception when account not found")
    void shouldThrowExceptionWhenAccountNotFound() {
        // Given
        Long nonExistentAccountNumber = 9876543217L;  // Valid Luhn but doesn't exist
        DepositRequest request = new DepositRequest(
                new BigDecimal("100.00"),
                PaymentProvider.MPESA,
                "MPESA-REF-12345"
        );

        // When & Then
        assertThatThrownBy(() -> paymentService.depositToAccount(nonExistentAccountNumber, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Account not found");
    }

    // =========================================================================
    // depositToMainAccount Tests
    // =========================================================================

    @Test
    @DisplayName("Should deposit to main account successfully")
    void shouldDepositToMainAccountSuccessfully() {
        // Given
        DepositRequest request = new DepositRequest(
                new BigDecimal("200.00"),
                PaymentProvider.MTN_MOMO,
                "MTN-REF-67890"
        );

        // When
        Payment payment = paymentService.depositToMainAccount(testCustomer.getCustomerId(), request);

        // Then
        assertThat(payment).isNotNull();
        assertThat(payment.getAccount().getAccountNumber()).isEqualTo(testAccount.getAccountNumber());
        assertThat(payment.getAmount()).isEqualByComparingTo(new BigDecimal("200.00"));
        assertThat(payment.getPaymentProvider()).isEqualTo(PaymentProvider.MTN_MOMO);

        // Verify balance was updated
        Account updatedAccount = accountRepository.findById(testAccount.getAccountNumber())
                .orElseThrow(() -> new AssertionError(
                        "Expected account " + testAccount.getAccountNumber() + " to exist after deposit to main account"
                ));
        assertThat(updatedAccount.getBalance()).isEqualByComparingTo(new BigDecimal("200.00"));
    }

    @Test
    @DisplayName("Should throw exception when customer not found")
    void shouldThrowExceptionWhenCustomerNotFound() {
        // Given
        Long nonExistentCustomerId = 99999997L;  // Valid Luhn but doesn't exist
        DepositRequest request = new DepositRequest(
                new BigDecimal("100.00"),
                PaymentProvider.MPESA,
                "MPESA-REF-12345"
        );

        // When & Then
        assertThatThrownBy(() -> paymentService.depositToMainAccount(nonExistentCustomerId, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("not found");
    }

    // =========================================================================
    // getPaymentByReference Tests
    // =========================================================================

    @Test
    @DisplayName("Should get payment by provider and reference")
    void shouldGetPaymentByProviderAndReference() {
        // Given - Create a payment
        DepositRequest request = new DepositRequest(
                new BigDecimal("150.00"),
                PaymentProvider.AIRTEL_MONEY,
                "AIRTEL-REF-99999"
        );
        Payment createdPayment = paymentService.depositToAccount(testAccount.getAccountNumber(), request);

        // When - Retrieve it by provider and reference
        Payment retrievedPayment = paymentService.getPaymentByReference(
                PaymentProvider.AIRTEL_MONEY,
                "AIRTEL-REF-99999"
        );

        // Then
        assertThat(retrievedPayment).isNotNull();
        assertThat(retrievedPayment.getReceiptNumber()).isEqualTo(createdPayment.getReceiptNumber());
        assertThat(retrievedPayment.getAmount()).isEqualByComparingTo(new BigDecimal("150.00"));
        assertThat(retrievedPayment.getPaymentProvider()).isEqualTo(PaymentProvider.AIRTEL_MONEY);
        assertThat(retrievedPayment.getPaymentReference()).isEqualTo("AIRTEL-REF-99999");
    }

    @Test
    @DisplayName("Should throw exception when payment not found")
    void shouldThrowExceptionWhenPaymentNotFound() {
        // When & Then
        assertThatThrownBy(() -> paymentService.getPaymentByReference(
                PaymentProvider.MPESA,
                "NON-EXISTENT-REF"
        ))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Payment not found");
    }

    // =========================================================================
    // Edge Cases
    // =========================================================================

    @Test
    @DisplayName("Should handle minimum deposit amount (0.01)")
    void shouldHandleMinimumDepositAmount() {
        // Given
        DepositRequest request = new DepositRequest(
                new BigDecimal("0.01"),
                PaymentProvider.MPESA,
                "MPESA-REF-MIN"
        );

        // When
        Payment payment = paymentService.depositToAccount(testAccount.getAccountNumber(), request);

        // Then
        assertThat(payment.getAmount()).isEqualByComparingTo(new BigDecimal("0.01"));

        Account updatedAccount = accountRepository.findById(testAccount.getAccountNumber())
                .orElseThrow(() -> new AssertionError(
                        "Expected account " + testAccount.getAccountNumber() + " to exist after minimum deposit"
                ));
        assertThat(updatedAccount.getBalance()).isEqualByComparingTo(new BigDecimal("0.01"));
    }

    @Test
    @DisplayName("Should handle large deposit amount")
    void shouldHandleLargeDepositAmount() {
        // Given
        DepositRequest request = new DepositRequest(
                new BigDecimal("9999999.99"),
                PaymentProvider.MTN_MOMO,
                "MTN-REF-LARGE"
        );

        // When
        Payment payment = paymentService.depositToAccount(testAccount.getAccountNumber(), request);

        // Then
        assertThat(payment.getAmount()).isEqualByComparingTo(new BigDecimal("9999999.99"));

        Account updatedAccount = accountRepository.findById(testAccount.getAccountNumber())
                .orElseThrow(() -> new AssertionError(
                        "Expected account " + testAccount.getAccountNumber() + " to exist after large deposit"
                ));
        assertThat(updatedAccount.getBalance()).isEqualByComparingTo(new BigDecimal("9999999.99"));
    }

    @Test
    @DisplayName("Should handle multiple sequential deposits")
    void shouldHandleMultipleSequentialDeposits() {
        // Given - Multiple deposits
        DepositRequest request1 = new DepositRequest(new BigDecimal("100.00"), PaymentProvider.MPESA, "REF-1");
        DepositRequest request2 = new DepositRequest(new BigDecimal("50.00"), PaymentProvider.MPESA, "REF-2");
        DepositRequest request3 = new DepositRequest(new BigDecimal("25.50"), PaymentProvider.MTN_MOMO, "REF-3");

        // When
        paymentService.depositToAccount(testAccount.getAccountNumber(), request1);
        paymentService.depositToAccount(testAccount.getAccountNumber(), request2);
        paymentService.depositToAccount(testAccount.getAccountNumber(), request3);

        // Then
        Account updatedAccount = accountRepository.findById(testAccount.getAccountNumber())
                .orElseThrow(() -> new AssertionError(
                        "Expected account " + testAccount.getAccountNumber() + " to exist after multiple deposits"
                ));
        assertThat(updatedAccount.getBalance()).isEqualByComparingTo(new BigDecimal("175.50"));

        // Verify all payments were created
        assertThat(paymentRepository.count()).isEqualTo(3);
    }

    // =========================================================================
    // Soft Delete Tests
    // =========================================================================

    @Test
    @DisplayName("Should throw exception when depositing to account of deleted customer (depositToAccount)")
    void shouldThrowExceptionWhenDepositingToAccountOfDeletedCustomer() {
        // Given - Delete the customer (soft delete)
        testCustomer.setActive(false);
        customerRepository.save(testCustomer);

        DepositRequest request = new DepositRequest(
                new BigDecimal("100.00"),
                PaymentProvider.MPESA,
                "MPESA-REF-DELETED"
        );

        // When & Then - Should not allow deposit to account of deleted customer
        assertThatThrownBy(() -> paymentService.depositToAccount(testAccount.getAccountNumber(), request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Account not found");
    }

    @Test
    @DisplayName("Should throw exception when depositing to main account of deleted customer (depositToMainAccount)")
    void shouldThrowExceptionWhenDepositingToMainAccountOfDeletedCustomer() {
        // Given - Delete the customer (soft delete)
        testCustomer.setActive(false);
        customerRepository.save(testCustomer);

        DepositRequest request = new DepositRequest(
                new BigDecimal("100.00"),
                PaymentProvider.MPESA,
                "MPESA-REF-DELETED"
        );

        // When & Then - Should not allow deposit to deleted customer
        assertThatThrownBy(() -> paymentService.depositToMainAccount(testCustomer.getCustomerId(), request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Main account not found");
    }
}