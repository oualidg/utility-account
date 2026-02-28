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
import com.mycompany.api.account.BaseWebMvcTest;
import com.mycompany.api.account.dto.DepositRequest;
import com.mycompany.api.account.dto.PaymentResponse;
import com.mycompany.api.account.entity.Account;
import com.mycompany.api.account.entity.Customer;
import com.mycompany.api.account.entity.Payment;
import com.mycompany.api.account.entity.PaymentProvider;
import com.mycompany.api.account.exception.ResourceNotFoundException;
import com.mycompany.api.account.mapper.PaymentMapper;
import com.mycompany.api.account.service.PaymentService;
import com.mycompany.api.account.service.ProviderService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
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
 *
 * <p>The authenticated provider is injected directly as a request attribute
 * to simulate what {@code ApiKeyAuthFilter} would set in production.</p>
 *
 * @author Oualid Gharach
 */
@WebMvcTest(PaymentController.class)
@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(properties = "spring.main.banner-mode=off")
@DisplayName("PaymentController Unit Tests")
class PaymentControllerTest extends BaseWebMvcTest {

    private static final String PROVIDER_ATTR = "authenticatedProvider";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PaymentService paymentService;

    @MockitoBean
    private PaymentMapper paymentMapper;

    @MockitoBean
    private ProviderService providerService;

    // =========================================================================
    // POST /api/v1/accounts/{accountNumber}/payments - Deposit to Account
    // =========================================================================

    @Test
    @DisplayName("Should deposit to account successfully")
    void shouldDepositToAccountSuccessfully() throws Exception {
        // Given
        Long accountNumber = 1234567897L;
        DepositRequest request = new DepositRequest(new BigDecimal("50.00"), "MPESA-REF-12345");

        Payment payment = createPayment(accountNumber, request, createMpesaProvider());
        PaymentResponse response = new PaymentResponse(
                "019c4d53-97f9-7b33-a607-e42f6916c810",
                accountNumber,
                new BigDecimal("50.00"),
                "MPESA", "M-Pesa", "MPESA-REF-12345",
                Instant.parse("2026-02-11T15:30:00Z")
        );

        when(paymentService.depositToAccount(eq(accountNumber), any(DepositRequest.class), any()))
                .thenReturn(payment);
        when(paymentMapper.toResponse(payment)).thenReturn(response);

        // When & Then
        mockMvc.perform(post("/api/v1/accounts/{accountNumber}/payments", accountNumber)
                        .requestAttr(PROVIDER_ATTR, createMpesaProvider())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.receiptNumber", is("019c4d53-97f9-7b33-a607-e42f6916c810")))
                .andExpect(jsonPath("$.accountNumber", is(1234567897)))
                .andExpect(jsonPath("$.amount", is(50.00)))
                .andExpect(jsonPath("$.providerCode", is("MPESA")))
                .andExpect(jsonPath("$.providerName", is("M-Pesa")))
                .andExpect(jsonPath("$.paymentReference", is("MPESA-REF-12345")));
    }

    @Test
    @DisplayName("Should return 400 when deposit amount is missing")
    void shouldReturn400WhenDepositAmountIsMissing() throws Exception {
        String invalidRequest = """
                {
                    "amount": null,
                    "paymentReference": "MPESA-REF-12345"
                }
                """;

        mockMvc.perform(post("/api/v1/accounts/{accountNumber}/payments", 1234567897L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidRequest))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Amount is required")));
    }

    @Test
    @DisplayName("Should return 400 when deposit amount is less than minimum")
    void shouldReturn400WhenDepositAmountIsTooSmall() throws Exception {
        DepositRequest request = new DepositRequest(new BigDecimal("0.00"), "MPESA-REF-12345");

        mockMvc.perform(post("/api/v1/accounts/{accountNumber}/payments", 1234567897L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Amount must be greater than zero")));
    }

    @Test
    @DisplayName("Should return 400 when payment reference is blank")
    void shouldReturn400WhenPaymentReferenceIsBlank() throws Exception {
        DepositRequest request = new DepositRequest(new BigDecimal("50.00"), "");

        mockMvc.perform(post("/api/v1/accounts/{accountNumber}/payments", 1234567897L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Payment reference is required")));
    }

    @Test
    @DisplayName("Should return 400 when account number fails Luhn validation")
    void shouldReturn400WhenAccountNumberFailsLuhnValidation() throws Exception {
        DepositRequest request = new DepositRequest(new BigDecimal("50.00"), "MPESA-REF-12345");

        mockMvc.perform(post("/api/v1/accounts/{accountNumber}/payments", 1234567891L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Account number must be a valid 10-digit number with checksum")));
    }

    @Test
    @DisplayName("Should return 400 when deposit amount exceeds maximum")
    void shouldReturn400WhenDepositAmountExceedsMaximum() throws Exception {
        DepositRequest request = new DepositRequest(new BigDecimal("99999999999999.99"), "MPESA-REF-12345");

        mockMvc.perform(post("/api/v1/accounts/{accountNumber}/payments", 1234567897L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 400 when deposit amount has more than 2 decimal places")
    void shouldReturn400WhenDepositAmountHasTooManyDecimals() throws Exception {
        DepositRequest request = new DepositRequest(new BigDecimal("10.001"), "MPESA-REF-12345");

        mockMvc.perform(post("/api/v1/accounts/{accountNumber}/payments", 1234567897L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Amount must have at most 2 decimal places")));
    }

    @Test
    @DisplayName("Should return 400 when deposit amount is negative")
    void shouldReturn400WhenDepositAmountIsNegative() throws Exception {
        DepositRequest request = new DepositRequest(new BigDecimal("-50.00"), "MPESA-REF-12345");

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
        Long customerId = 12345674L;
        DepositRequest request = new DepositRequest(new BigDecimal("100.00"), "MTN-REF-67890");

        Payment payment = createPayment(1234567897L, request, createMtnProvider());
        PaymentResponse response = new PaymentResponse(
                "019c4d53-97f9-7b33-a607-e42f6916c810",
                1234567897L,
                new BigDecimal("100.00"),
                "MTN", "MTN Mobile Money", "MTN-REF-67890",
                Instant.parse("2026-02-11T15:30:00Z")
        );

        when(paymentService.depositToMainAccount(eq(customerId), any(DepositRequest.class), any()))
                .thenReturn(payment);
        when(paymentMapper.toResponse(payment)).thenReturn(response);

        // When & Then
        mockMvc.perform(post("/api/v1/customers/{customerId}/payments", customerId)
                        .requestAttr(PROVIDER_ATTR, createMtnProvider())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.receiptNumber", is("019c4d53-97f9-7b33-a607-e42f6916c810")))
                .andExpect(jsonPath("$.providerCode", is("MTN")))
                .andExpect(jsonPath("$.paymentReference", is("MTN-REF-67890")));
    }

    @Test
    @DisplayName("Should return 400 when customer ID fails Luhn validation")
    void shouldReturn400WhenCustomerIdFailsLuhnValidation() throws Exception {
        DepositRequest request = new DepositRequest(new BigDecimal("50.00"), "MPESA-REF-12345");

        mockMvc.perform(post("/api/v1/customers/{customerId}/payments", 12345671L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Customer ID must be a valid 8-digit number with checksum")));
    }

    @Test
    @DisplayName("Should return 404 when customer not found")
    void shouldReturn404WhenCustomerNotFound() throws Exception {
        // Given
        Long customerId = 99999997L;
        DepositRequest request = new DepositRequest(new BigDecimal("50.00"), "MPESA-REF-12345");

        when(paymentService.depositToMainAccount(eq(customerId), any(DepositRequest.class), any()))
                .thenThrow(new ResourceNotFoundException("Customer not found with ID: " + customerId));

        // When & Then
        mockMvc.perform(post("/api/v1/customers/{customerId}/payments", customerId)
                        .requestAttr(PROVIDER_ATTR, createMpesaProvider())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", is("Customer not found with ID: " + customerId)));
    }

    @Test
    @DisplayName("Should return 422 when balance update fails")
    void shouldReturn422WhenBalanceUpdateFails() throws Exception {
        // Given
        Long accountNumber = 1234567897L;
        DepositRequest request = new DepositRequest(new BigDecimal("50.00"), "MPESA-REF-OVERFLOW");

        when(paymentService.depositToAccount(eq(accountNumber), any(DepositRequest.class), any()))
                .thenThrow(new com.mycompany.api.account.exception.BalanceUpdateException(
                        "Balance update failed for account: " + accountNumber));

        // When & Then
        mockMvc.perform(post("/api/v1/accounts/{accountNumber}/payments", accountNumber)
                        .requestAttr(PROVIDER_ATTR, createMpesaProvider())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message", is("Balance update failed for account: " + accountNumber)));
    }

    // =========================================================================
    // GET /api/v1/payments/confirmation/{reference}
    // =========================================================================

    @Test
    @DisplayName("Should confirm payment successfully")
    void shouldConfirmPaymentSuccessfully() throws Exception {
        // Given
        String reference = "MPESA-REF-12345";

        Payment payment = createPayment(1234567890L,
                new DepositRequest(new BigDecimal("50.00"), reference),
                createMpesaProvider());
        PaymentResponse response = new PaymentResponse(
                "019c4d53-97f9-7b33-a607-e42f6916c810",
                1234567890L,
                new BigDecimal("50.00"),
                "MPESA", "M-Pesa", reference,
                Instant.parse("2026-02-11T15:30:00Z")
        );

        when(paymentService.getPaymentByReference(any(), eq(reference))).thenReturn(payment);
        when(paymentMapper.toResponse(payment)).thenReturn(response);

        // When & Then
        mockMvc.perform(get("/api/v1/payments/confirmation/{reference}", reference)
                        .requestAttr(PROVIDER_ATTR, createMpesaProvider()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.receiptNumber", is("019c4d53-97f9-7b33-a607-e42f6916c810")))
                .andExpect(jsonPath("$.providerCode", is("MPESA")))
                .andExpect(jsonPath("$.paymentReference", is("MPESA-REF-12345")));
    }

    @Test
    @DisplayName("Should return 404 when payment not found")
    void shouldReturn404WhenPaymentNotFound() throws Exception {
        // Given
        String reference = "NON-EXISTENT-REF";

        when(paymentService.getPaymentByReference(any(), eq(reference)))
                .thenThrow(new ResourceNotFoundException(
                        "Payment not found with provider MPESA and reference " + reference));

        // When & Then
        mockMvc.perform(get("/api/v1/payments/confirmation/{reference}", reference)
                        .requestAttr(PROVIDER_ATTR, createMpesaProvider()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message",
                        is("Payment not found with provider MPESA and reference " + reference)));
    }

    // =========================================================================
    // Helper Methods
    // =========================================================================

    private PaymentProvider createMpesaProvider() {
        PaymentProvider provider = new PaymentProvider();
        provider.setId(1L);
        provider.setCode("MPESA");
        provider.setName("M-Pesa");
        provider.setApiKeyHash("hash");
        provider.setApiKeyPrefix("e7e75fe1");
        provider.setActive(true);
        return provider;
    }

    private PaymentProvider createMtnProvider() {
        PaymentProvider provider = new PaymentProvider();
        provider.setId(2L);
        provider.setCode("MTN");
        provider.setName("MTN Mobile Money");
        provider.setApiKeyHash("hash");
        provider.setApiKeyPrefix("0df87c1d");
        provider.setActive(true);
        return provider;
    }

    private Payment createPayment(Long accountNumber, DepositRequest request, PaymentProvider provider) {
        Customer customer = new Customer();
        customer.setCustomerId(12345674L);

        Account account = new Account();
        account.setAccountNumber(accountNumber);
        account.setCustomer(customer);
        account.setBalance(new BigDecimal("100.00"));

        Payment payment = new Payment();
        payment.setReceiptNumber("019c4d53-97f9-7b33-a607-e42f6916c810");
        payment.setAccount(account);
        payment.setAmount(request.amount());
        payment.setPaymentProvider(provider);
        payment.setPaymentReference(request.paymentReference());
        payment.setPaymentDate(Instant.parse("2026-02-11T15:30:00Z"));

        return payment;
    }
}