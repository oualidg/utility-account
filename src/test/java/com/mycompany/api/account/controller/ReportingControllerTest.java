/*
 * Copyright (c) 2026 Oualid Gharach. All rights reserved.
 *
 * Aiming for production-grade standards through clean code and best practices
 * for educational and informational purposes.
 *
 * Created on: 2/20/2026 at 7:11 PM
 *
 * Feel free to use or contribute. Contact: oualid.gharach@gmail.com
 */
package com.mycompany.api.account.controller;

import com.mycompany.api.account.dto.PaymentResponse;
import com.mycompany.api.account.dto.PaymentSummaryResponse;
import com.mycompany.api.account.dto.ProviderBreakdownResponse;
import com.mycompany.api.account.dto.ProviderReconciliationResponse;
import com.mycompany.api.account.exception.ResourceNotFoundException;
import com.mycompany.api.account.service.PaymentQueryService;
import com.mycompany.api.account.service.ProviderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

@WebMvcTest(ReportingController.class)
@TestPropertySource(properties = "spring.main.banner-mode=off")
@DisplayName("ReportingController Unit Tests")
class ReportingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PaymentQueryService paymentQueryService;

    @MockitoBean
    private ProviderService providerService;

    private PaymentResponse samplePayment;

    @BeforeEach
    void setUp() {
        samplePayment = new PaymentResponse(
                "receipt-uuid-001",
                1234567897L,
                new BigDecimal("150.00"),
                "MPESA",
                "M-Pesa",
                "REF-001",
                Instant.parse("2026-02-01T10:00:00Z")
        );
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/reports/payments
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Should return payments for valid search filters")
    void shouldReturnPaymentsForValidFilters() throws Exception {
        when(paymentQueryService.searchPayments(any(), any(), any(), any(), any()))
                .thenReturn(List.of(samplePayment));

        mockMvc.perform(get("/api/v1/reports/payments")
                        .param("providerCode", "MPESA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].receiptNumber", is("receipt-uuid-001")))
                .andExpect(jsonPath("$[0].providerCode", is("MPESA")))
                .andExpect(jsonPath("$[0].amount", is(150.00)));
    }

    @Test
    @DisplayName("Should return empty list when no payments match")
    void shouldReturnEmptyListWhenNoPaymentsMatch() throws Exception {
        when(paymentQueryService.searchPayments(any(), any(), any(), any(), any()))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/v1/reports/payments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("Should return 400 when account number fails Luhn validation")
    void shouldReturn400WhenAccountNumberInvalid() throws Exception {
        mockMvc.perform(get("/api/v1/reports/payments")
                        .param("accountNumber", "1234567891"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message",
                        is("Account number must be a valid 10-digit number with checksum")));
    }

    @Test
    @DisplayName("Should return 400 when customer ID fails Luhn validation")
    void shouldReturn400WhenCustomerIdInvalid() throws Exception {
        mockMvc.perform(get("/api/v1/reports/payments")
                        .param("customerId", "12345671"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message",
                        is("Customer ID must be a valid 8-digit number with checksum")));
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/reports/summary
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Should return dashboard summary")
    void shouldReturnDashboardSummary() throws Exception {
        PaymentSummaryResponse summary = new PaymentSummaryResponse(
                new BigDecimal("500.00"),
                3L,
                List.of(new ProviderBreakdownResponse("MPESA", "M-Pesa", new BigDecimal("500.00"), 3L))
        );

        when(paymentQueryService.getDashboardSummary(any(), any())).thenReturn(summary);

        mockMvc.perform(get("/api/v1/reports/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAmount", is(500.00)))
                .andExpect(jsonPath("$.totalCount", is(3)))
                .andExpect(jsonPath("$.byProvider", hasSize(1)))
                .andExpect(jsonPath("$.byProvider[0].providerCode", is("MPESA")))
                .andExpect(jsonPath("$.byProvider[0].count", is(3)));
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/reports/accounts/{accountNumber}/summary
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Should return account summary")
    void shouldReturnAccountSummary() throws Exception {
        PaymentSummaryResponse summary = new PaymentSummaryResponse(
                new BigDecimal("250.00"), 2L, List.of()
        );

        when(paymentQueryService.getAccountSummary(1234567897L)).thenReturn(summary);

        mockMvc.perform(get("/api/v1/reports/accounts/1234567897/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAmount", is(250.00)))
                .andExpect(jsonPath("$.totalCount", is(2)));
    }

    @Test
    @DisplayName("Should return 400 when account number fails Luhn validation")
    void shouldReturn400OnAccountSummaryWithInvalidLuhn() throws Exception {
        mockMvc.perform(get("/api/v1/reports/accounts/1234567891/summary"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message",
                        is("Account number must be a valid 10-digit number with checksum")));
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/reports/providers/{providerCode}/reconciliation
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Should return reconciliation report for valid provider")
    void shouldReturnReconciliationReport() throws Exception {
        ProviderReconciliationResponse reconciliation = new ProviderReconciliationResponse(
                "MPESA", "M-Pesa", new BigDecimal("150.00"), 1L, List.of(samplePayment)
        );

        when(paymentQueryService.getReconciliation(eq("MPESA"), any(), any()))
                .thenReturn(reconciliation);

        mockMvc.perform(get("/api/v1/reports/providers/MPESA/reconciliation"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.providerCode", is("MPESA")))
                .andExpect(jsonPath("$.providerName", is("M-Pesa")))
                .andExpect(jsonPath("$.totalAmount", is(150.00)))
                .andExpect(jsonPath("$.totalCount", is(1)))
                .andExpect(jsonPath("$.payments", hasSize(1)));
    }

    @Test
    @DisplayName("Should return 404 when provider not found")
    void shouldReturn404WhenProviderNotFound() throws Exception {
        when(paymentQueryService.getReconciliation(eq("UNKNOWN"), any(), any()))
                .thenThrow(new ResourceNotFoundException("Provider not found: UNKNOWN"));

        mockMvc.perform(get("/api/v1/reports/providers/UNKNOWN/reconciliation"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", is("Provider not found: UNKNOWN")));
    }
}