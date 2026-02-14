/*
 * Copyright (c) 2026 Oualid Gharach. All rights reserved.
 *
 * Aiming for production-grade standards through clean code and best practices
 * for educational and informational purposes.
 *
 * Created on: 2/11/2026 at 7:24 PM
 *
 * Feel free to use or contribute. Contact: oualid.gharach@gmail.com
 */
package com.mycompany.api.account.mapper;

import com.mycompany.api.account.dto.PaymentResponse;
import com.mycompany.api.account.entity.Account;
import com.mycompany.api.account.entity.Customer;
import com.mycompany.api.account.entity.Payment;
import com.mycompany.api.account.model.PaymentProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for PaymentMapper.
 * Tests MapStruct mapping between Payment entity and PaymentResponse DTO.
 *
 * @author Oualid Gharach
 */
@DisplayName("PaymentMapper Unit Tests")
class PaymentMapperTest {

    private PaymentMapper paymentMapper;

    @BeforeEach
    void setUp() {
        // Get MapStruct generated implementation
        paymentMapper = Mappers.getMapper(PaymentMapper.class);
    }

    @Test
    @DisplayName("Should map Payment entity to PaymentResponse with all fields")
    void shouldMapPaymentToResponse() {
        // Given
        Payment payment = createBasicPayment();

        // When
        PaymentResponse response = paymentMapper.toResponse(payment);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.receiptNumber()).isEqualTo("019c4d54-0000-0000-0000-000000000000");
        assertThat(response.accountNumber())
                .as("Should extract account number from nested account entity")
                .isEqualTo(1234567890L);
        assertThat(response.amount()).isEqualByComparingTo(new BigDecimal("10.00"));
        assertThat(response.paymentProvider()).isEqualTo(PaymentProvider.MPESA);
        assertThat(response.paymentReference()).isEqualTo("TEST-REF");
        assertThat(response.paymentDate())
                .as("Payment date should be exact Instant without timezone conversion")
                .isEqualTo(Instant.parse("2026-02-11T10:15:30.123456789Z"));

    }

    @Test
    @DisplayName("Should map all payment providers correctly")
    void shouldMapAllPaymentProviders() {
        // Test MPESA
        Payment mpesaPayment = createPaymentWithProvider(PaymentProvider.MPESA);
        PaymentResponse mpesaResponse = paymentMapper.toResponse(mpesaPayment);
        assertThat(mpesaResponse.paymentProvider()).isEqualTo(PaymentProvider.MPESA);

        // Test MTN_MOMO
        Payment mtnPayment = createPaymentWithProvider(PaymentProvider.MTN_MOMO);
        PaymentResponse mtnResponse = paymentMapper.toResponse(mtnPayment);
        assertThat(mtnResponse.paymentProvider()).isEqualTo(PaymentProvider.MTN_MOMO);

        // Test AIRTEL_MONEY
        Payment airtelPayment = createPaymentWithProvider(PaymentProvider.AIRTEL_MONEY);
        PaymentResponse airtelResponse = paymentMapper.toResponse(airtelPayment);
        assertThat(airtelResponse.paymentProvider()).isEqualTo(PaymentProvider.AIRTEL_MONEY);
    }

    @Test
    @DisplayName("Should preserve decimal precision for amounts")
    void shouldPreserveDecimalPrecision() {
        // Given - Test various decimal amounts
        BigDecimal[] testAmounts = {
                new BigDecimal("0.01"),      // Minimum
                new BigDecimal("100.00"),    // Whole number
                new BigDecimal("123.45"),    // Standard decimal
                new BigDecimal("999.99"),    // Near maximum
                new BigDecimal("1234567.89") // Large amount
        };

        for (BigDecimal testAmount : testAmounts) {
            Payment payment = createPaymentWithAmount(testAmount);

            // When
            PaymentResponse response = paymentMapper.toResponse(payment);

            // Then
            assertThat(response.amount())
                    .as("Amount %s should be preserved exactly", testAmount)
                    .isEqualByComparingTo(testAmount);
        }
    }

    @Test
    @DisplayName("Should handle null payment gracefully")
    void shouldHandleNullPayment() {
        // When
        PaymentResponse response = paymentMapper.toResponse(null);

        // Then
        assertThat(response).isNull();
    }


    // =========================================================================
    // Helper Methods
    // =========================================================================

    private Payment createBasicPayment() {
        Customer customer = new Customer();
        customer.setCustomerId(12345670L);

        Account account = new Account();
        account.setAccountNumber(1234567890L);
        account.setCustomer(customer);
        account.setBalance(BigDecimal.ZERO);

        Payment payment = new Payment();
        payment.setReceiptNumber("019c4d54-0000-0000-0000-000000000000");
        payment.setAccount(account);
        payment.setAmount(new BigDecimal("10.00"));
        payment.setPaymentProvider(PaymentProvider.MPESA);
        payment.setPaymentReference("TEST-REF");
        payment.setPaymentDate(Instant.parse("2026-02-11T10:15:30.123456789Z"));

        return payment;
    }

    private Payment createPaymentWithProvider(PaymentProvider provider) {
        Payment payment = createBasicPayment();
        payment.setPaymentProvider(provider);
        return payment;
    }

    private Payment createPaymentWithAmount(BigDecimal amount) {
        Payment payment = createBasicPayment();
        payment.setAmount(amount);
        return payment;
    }
}