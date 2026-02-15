/*
 * Copyright (c) 2026 Oualid Gharach. All rights reserved.
 *
 * Aiming for production-grade standards through clean code and best practices
 * for educational and informational purposes.
 *
 * Created on: 2/11/2026 at 9:36 PM
 *
 * Feel free to use or contribute. Contact: oualid.gharach@gmail.com
 */
package com.mycompany.api.account.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mycompany.api.account.dto.DepositRequest;
import com.mycompany.api.account.dto.PaymentResponse;
import com.mycompany.api.account.entity.Account;
import com.mycompany.api.account.entity.Customer;
import com.mycompany.api.account.entity.Payment;
import com.mycompany.api.account.exception.ResourceNotFoundException;
import com.mycompany.api.account.mapper.PaymentMapper;
import com.mycompany.api.account.model.PaymentProvider;
import com.mycompany.api.account.service.PaymentService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for PaymentController.
 * Uses @WebMvcTest to test only the web layer with mocked service.
 *
 * @author Oualid Gharach
 */
@WebMvcTest(PaymentController.class)
@TestPropertySource(properties = "spring.main.banner-mode=off")
@DisplayName("PaymentController Unit Tests")
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PaymentService paymentService;

    @MockitoBean
    private PaymentMapper paymentMapper;

    // Real mapper instance for creating test responses
    private final PaymentMapper realMapper = Mappers.getMapper(PaymentMapper.class);

    // =========================================================================
    // POST /api/v1/accounts/{accountNumber}/payments - Deposit to Account
    // =========================================================================

    @Test
    @DisplayName("Should deposit to account successfully")
    void shouldDepositToAccountSuccessfully() throws Exception {
        // Given
        Long accountNumber = 1234567897L;  // Valid Luhn
        DepositRequest request = new DepositRequest(
                new BigDecimal("50.00"),
                PaymentProvider.MPESA,
                "MPESA-REF-12345"
        );

        Payment payment = createPayment(accountNumber, request);
        PaymentResponse response = createPaymentResponse(payment);

        when(paymentService.depositToAccount(eq(accountNumber), any(DepositRequest.class)))
                .thenReturn(payment);
        when(paymentMapper.toResponse(payment)).thenReturn(response);

        // When & Then
        mockMvc.perform(post("/api/v1/accounts/{accountNumber}/payments", accountNumber)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.receiptNumber", is("019c4d53-97f9-7b33-a607-e42f6916c810")))
                .andExpect(jsonPath("$.accountNumber", is(1234567897)))
                .andExpect(jsonPath("$.amount", is(50.00)))
                .andExpect(jsonPath("$.paymentProvider", is("MPESA")))
                .andExpect(jsonPath("$.paymentReference", is("MPESA-REF-12345")));
    }

    @Test
    @DisplayName("Should return 400 when deposit amount is missing")
    void shouldReturn400WhenDepositAmountIsMissing() throws Exception {
        // Given - Request with null amount
        String invalidRequest = """
                {
                    "amount": null,
                    "paymentProvider": "MPESA",
                    "paymentReference": "MPESA-REF-12345"
                }
                """;

        // When & Then
        mockMvc.perform(post("/api/v1/accounts/{accountNumber}/payments", 1234567897L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidRequest))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Amount is required")));
    }

    @Test
    @DisplayName("Should return 400 when deposit amount is less than minimum")
    void shouldReturn400WhenDepositAmountIsTooSmall() throws Exception {
        // Given - Request with amount < 0.01
        DepositRequest request = new DepositRequest(
                new BigDecimal("0.00"),
                PaymentProvider.MPESA,
                "MPESA-REF-12345"
        );

        // When & Then
        mockMvc.perform(post("/api/v1/accounts/{accountNumber}/payments", 1234567897L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Amount must be greater than zero")));
    }

    @Test
    @DisplayName("Should return 400 when payment provider is missing")
    void shouldReturn400WhenPaymentProviderIsMissing() throws Exception {
        // Given - Request with null provider
        String invalidRequest = """
                {
                    "amount": 50.00,
                    "paymentProvider": null,
                    "paymentReference": "MPESA-REF-12345"
                }
                """;

        // When & Then
        mockMvc.perform(post("/api/v1/accounts/{accountNumber}/payments", 1234567897L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidRequest))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Payment provider is required")));
    }

    @Test
    @DisplayName("Should return 400 when payment reference is blank")
    void shouldReturn400WhenPaymentReferenceIsBlank() throws Exception {
        // Given - Request with blank reference
        DepositRequest request = new DepositRequest(
                new BigDecimal("50.00"),
                PaymentProvider.MPESA,
                ""
        );

        // When & Then
        mockMvc.perform(post("/api/v1/accounts/{accountNumber}/payments", 1234567897L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Payment reference is required")));
    }

    @Test
    @DisplayName("Should return 400 when account number fails Luhn validation")
    void shouldReturn400WhenAccountNumberFailsLuhnValidation() throws Exception {
        // Given - Invalid Luhn account number
        Long invalidAccountNumber = 1234567891L; // Invalid checksum
        DepositRequest request = new DepositRequest(
                new BigDecimal("50.00"),
                PaymentProvider.MPESA,
                "MPESA-REF-12345"
        );

        // When & Then
        mockMvc.perform(post("/api/v1/accounts/{accountNumber}/payments", invalidAccountNumber)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Account number must be a valid 10-digit number with checksum")));
    }

    @Test
    @DisplayName("Should return 400 when deposit amount exceeds maximum")
    void shouldReturn400WhenDepositAmountExceedsMaximum() throws Exception {
        // Given - Amount exceeds DECIMAL(15,2) capacity
        DepositRequest request = new DepositRequest(
                new BigDecimal("99999999999999.99"),
                PaymentProvider.MPESA,
                "MPESA-REF-12345"
        );

        // When & Then
        mockMvc.perform(post("/api/v1/accounts/{accountNumber}/payments", 1234567897L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 400 when deposit amount has more than 2 decimal places")
    void shouldReturn400WhenDepositAmountHasTooManyDecimals() throws Exception {
        // Given - 3 decimal places (would silently truncate in DB without validation)
        DepositRequest request = new DepositRequest(
                new BigDecimal("10.001"),
                PaymentProvider.MPESA,
                "MPESA-REF-12345"
        );

        // When & Then
        mockMvc.perform(post("/api/v1/accounts/{accountNumber}/payments", 1234567897L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Amount must have at most 2 decimal places")));
    }

    @Test
    @DisplayName("Should return 400 when deposit amount is negative")
    void shouldReturn400WhenDepositAmountIsNegative() throws Exception {
        // Given
        DepositRequest request = new DepositRequest(
                new BigDecimal("-50.00"),
                PaymentProvider.MPESA,
                "MPESA-REF-12345"
        );

        // When & Then
        mockMvc.perform(post("/api/v1/accounts/{accountNumber}/payments", 1234567897L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Amount must be greater than zero")));
    }

    // =========================================================================
    // POST /api/v1/customers/{customerId}/payments - Deposit to Main Account
    // =========================================================================

    @Test
    @DisplayName("Should deposit to main account successfully")
    void shouldDepositToMainAccountSuccessfully() throws Exception {
        // Given
        Long customerId = 12345674L;  // Valid Luhn (matches CustomerController)
        DepositRequest request = new DepositRequest(
                new BigDecimal("100.00"),
                PaymentProvider.MTN_MOMO,
                "MTN-REF-67890"
        );

        Payment payment = createPayment(1234567897L, request);  // Valid Luhn account number
        PaymentResponse response = createPaymentResponse(payment);

        when(paymentService.depositToMainAccount(eq(customerId), any(DepositRequest.class)))
                .thenReturn(payment);
        when(paymentMapper.toResponse(payment)).thenReturn(response);

        // When & Then
        mockMvc.perform(post("/api/v1/customers/{customerId}/payments", customerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.receiptNumber", is("019c4d53-97f9-7b33-a607-e42f6916c810")))
                .andExpect(jsonPath("$.paymentProvider", is("MTN_MOMO")))
                .andExpect(jsonPath("$.paymentReference", is("MTN-REF-67890")));
    }

    @Test
    @DisplayName("Should return 400 when customer ID fails Luhn validation")
    void shouldReturn400WhenCustomerIdFailsLuhnValidation() throws Exception {
        // Given - Invalid Luhn customer ID
        Long invalidCustomerId = 12345671L; // Invalid checksum
        DepositRequest request = new DepositRequest(
                new BigDecimal("50.00"),
                PaymentProvider.MPESA,
                "MPESA-REF-12345"
        );

        // When & Then
        mockMvc.perform(post("/api/v1/customers/{customerId}/payments", invalidCustomerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Customer ID must be a valid 8-digit number with checksum")));
    }

    @Test
    @DisplayName("Should return 404 when customer not found")
    void shouldReturn404WhenCustomerNotFound() throws Exception {
        // Given
        Long customerId = 99999997L; // Valid Luhn but doesn't exist
        DepositRequest request = new DepositRequest(
                new BigDecimal("50.00"),
                PaymentProvider.MPESA,
                "MPESA-REF-12345"
        );

        when(paymentService.depositToMainAccount(eq(customerId), any(DepositRequest.class)))
                .thenThrow(new ResourceNotFoundException("Customer not found with ID: " + customerId));

        // When & Then
        mockMvc.perform(post("/api/v1/customers/{customerId}/payments", customerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", is("Customer not found with ID: " + customerId)));
    }

    // =========================================================================
    // GET /api/v1/payments/confirmation/{provider}/{reference} - Confirm Payment
    // =========================================================================

    @Test
    @DisplayName("Should confirm payment successfully")
    void shouldConfirmPaymentSuccessfully() throws Exception {
        // Given
        PaymentProvider provider = PaymentProvider.MPESA;
        String reference = "MPESA-REF-12345";

        Payment payment = createPayment(1234567890L, new DepositRequest(
                new BigDecimal("50.00"),
                provider,
                reference
        ));
        PaymentResponse response = createPaymentResponse(payment);

        when(paymentService.getPaymentByReference(provider, reference)).thenReturn(payment);
        when(paymentMapper.toResponse(payment)).thenReturn(response);

        // When & Then
        mockMvc.perform(get("/api/v1/payments/confirmation/{provider}/{reference}", provider, reference))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.receiptNumber", is("019c4d53-97f9-7b33-a607-e42f6916c810")))
                .andExpect(jsonPath("$.paymentProvider", is("MPESA")))
                .andExpect(jsonPath("$.paymentReference", is("MPESA-REF-12345")));
    }

    @Test
    @DisplayName("Should return 404 when payment not found")
    void shouldReturn404WhenPaymentNotFound() throws Exception {
        // Given
        PaymentProvider provider = PaymentProvider.MPESA;
        String reference = "NON-EXISTENT-REF";

        when(paymentService.getPaymentByReference(provider, reference))
                .thenThrow(new ResourceNotFoundException("Payment not found with provider: " + provider + " and reference: " + reference));

        // When & Then
        mockMvc.perform(get("/api/v1/payments/confirmation/{provider}/{reference}", provider, reference))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", is("Payment not found with provider: " + provider + " and reference: " + reference)));
    }

    @Test
    @DisplayName("Should return 400 when payment provider is invalid")
    void shouldReturn400WhenPaymentProviderIsInvalid() throws Exception {
        // Given - Invalid enum value
        String invalidProvider = "PAYPAL";
        String reference = "PAYPAL-REF-12345";

        // When & Then
        mockMvc.perform(get("/api/v1/payments/confirmation/{provider}/{reference}", invalidProvider, reference))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Invalid value provided for parameter: 'provider'")));
    }

    // =========================================================================
    // Helper Methods
    // =========================================================================

    private Payment createPayment(Long accountNumber, DepositRequest request) {
        Customer customer = new Customer();
        customer.setCustomerId(12345674L);  // Valid Luhn

        Account account = new Account();
        account.setAccountNumber(accountNumber);
        account.setCustomer(customer);
        account.setBalance(new BigDecimal("100.00"));

        Payment payment = new Payment();
        payment.setReceiptNumber("019c4d53-97f9-7b33-a607-e42f6916c810");
        payment.setAccount(account);
        payment.setAmount(request.amount());
        payment.setPaymentProvider(request.paymentProvider());
        payment.setPaymentReference(request.paymentReference());
        payment.setPaymentDate(Instant.parse("2026-02-11T15:30:00Z"));

        return payment;
    }

    private PaymentResponse createPaymentResponse(Payment payment) {
        return realMapper.toResponse(payment);
    }
}