/*
 * Copyright (c) 2026 Oualid Gharach. All rights reserved.
 *
 * Aiming for production-grade standards through clean code and best practices
 * for educational and informational purposes.
 *
 * Created on: 2/20/2026 at 6:18 PM
 *
 * Feel free to use or contribute. Contact: oualid.gharach@gmail.com
 */
package com.mycompany.api.account.controller;

import com.mycompany.api.account.dto.PaymentResponse;
import com.mycompany.api.account.dto.PaymentSummaryResponse;
import com.mycompany.api.account.dto.ProviderReconciliationResponse;
import com.mycompany.api.account.service.PaymentQueryService;
import com.mycompany.api.account.validation.ValidLuhn;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

/**
 * REST controller for payment reporting endpoints.
 * All endpoints are read-only — no state is modified.
 *
 * @author Oualid Gharach
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
@Tag(name = "Reports", description = "Payment search, dashboard summaries, and provider reconciliation")
public class ReportingController {

    private final PaymentQueryService paymentQueryService;

    /**
     * Search payments with optional filters.
     * All parameters are optional — omit any to broaden the search.
     */
    @GetMapping("/payments")
    @Operation(summary = "Search payments", description = "Search payments by account, customer, provider, or date range")
    public ResponseEntity<List<PaymentResponse>> searchPayments(
            @RequestParam(required = false)
            @ValidLuhn(length = 10, message = "Account number must be a valid 10-digit number with checksum")
            Long accountNumber,

            @RequestParam(required = false)
            @ValidLuhn(length = 8, message = "Customer ID must be a valid 8-digit number with checksum")
            Long customerId,

            @RequestParam(required = false) String providerCode,

            @RequestParam(required = false) Instant from,

            @RequestParam(required = false) Instant to) {

        log.info("Search payments request: accountNumber={}, customerId={}, providerCode={}, from={}, to={}",
                accountNumber, customerId, providerCode, from, to);

        return ResponseEntity.ok(
                paymentQueryService.searchPayments(accountNumber, customerId, providerCode, from, to)
        );
    }

    /**
     * Global summary: total payment volume and count, broken down by provider.
     */
    @GetMapping("/summary")
    @Operation(summary = "Payment summary", description = "Total payment volume and count, broken down by provider")
    public ResponseEntity<PaymentSummaryResponse> getDashboardSummary(
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to) {

        log.info("Payment summary request: from={}, to={}", from, to);

        return ResponseEntity.ok(paymentQueryService.getDashboardSummary(from, to));
    }

    /**
     * Per-account payment summary.
     */
    @GetMapping("/accounts/{accountNumber}/summary")
    @Operation(summary = "Account summary", description = "Total payment amount and count for a specific account")
    public ResponseEntity<PaymentSummaryResponse> getAccountSummary(
            @PathVariable
            @ValidLuhn(length = 10, message = "Account number must be a valid 10-digit number with checksum")
            Long accountNumber) {

        log.info("Account summary request: accountNumber={}", accountNumber);

        return ResponseEntity.ok(paymentQueryService.getAccountSummary(accountNumber));
    }

    /**
     * Provider reconciliation report.
     */
    @GetMapping("/providers/{providerCode}/reconciliation")
    @Operation(summary = "Provider reconciliation", description = "Settlement report for a provider within a date range")
    public ResponseEntity<ProviderReconciliationResponse> getReconciliation(
            @PathVariable String providerCode,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to) {

        log.info("Reconciliation request: providerCode={}, from={}, to={}", providerCode, from, to);

        return ResponseEntity.ok(paymentQueryService.getReconciliation(providerCode, from, to));
    }
}