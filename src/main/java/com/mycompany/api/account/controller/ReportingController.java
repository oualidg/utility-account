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
import com.mycompany.api.account.dto.ProviderSummaryResponse;
import com.mycompany.api.account.service.PaymentQueryService;
import com.mycompany.api.account.validation.ValidLuhn;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

/**
 * REST controller for payment reporting endpoints.
 * All endpoints are read-only — no state is modified.
 *
 * <p>Access rules:
 * <ul>
 *   <li>Default — ADMIN only (class-level)</li>
 *   <li>{@code GET /payments} — overridden to allow ADMIN and OPERATOR,
 *       as operators need this to view account payment history</li>
 *   <li>{@code GET /providers/{code}/summary} — overridden to allow ADMIN and OPERATOR,
 *       as operators need this to view provider totals on the provider detail page</li>
 * </ul>
 *
 * @author Oualid Gharach
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Reports", description = "Payment search, dashboard summaries, and provider reconciliation")
public class ReportingController {

    private final PaymentQueryService paymentQueryService;

    /**
     * Search payments with optional filters.
     * Overrides the class-level ADMIN restriction — operators need this to view account payment history.
     * All parameters are optional — omit any to broaden the search.
     *
     * @param accountNumber optional account number filter
     * @param customerId    optional customer ID filter
     * @param providerCode  optional provider code filter
     * @param receiptNumber optional receipt number prefix filter (case-insensitive)
     * @param from          optional start of date range (inclusive)
     * @param to            optional end of date range (inclusive)
     * @return list of matching payment responses
     */
    @GetMapping("/payments")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    @Operation(summary = "Search payments", description = "Search payments by account, customer, provider, receipt, or date range")
    public ResponseEntity<List<PaymentResponse>> searchPayments(
            @RequestParam(required = false)
            @ValidLuhn(length = 10, message = "Account number must be a valid 10-digit number with checksum")
            Long accountNumber,

            @RequestParam(required = false)
            @ValidLuhn(length = 8, message = "Customer ID must be a valid 8-digit number with checksum")
            Long customerId,

            @RequestParam(required = false) String providerCode,

            // [ADDED] receipt prefix filter — % appended in repository, not here
            @RequestParam(required = false) String receiptNumber,

            @RequestParam(required = false) Instant from,

            @RequestParam(required = false) Instant to) {

        log.info("Search payments request: accountNumber={}, customerId={}, providerCode={}, receiptNumber={}, from={}, to={}",
                accountNumber, customerId, providerCode, receiptNumber, from, to);

        return ResponseEntity.ok(
                paymentQueryService.searchPayments(accountNumber, customerId, providerCode, receiptNumber, from, to)
        );
    }

    /**
     * Global summary: total payment volume and count, broken down by provider.
     * Used by the dashboard.
     *
     * @param from optional start of date range (inclusive)
     * @param to   optional end of date range (inclusive)
     * @return summary with totals and per-provider breakdown
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
     *
     * @param accountNumber the account number
     * @return summary with totals and per-provider breakdown
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
     * Lightweight provider summary for a given period.
     * Returns only totalCount and totalAmount — no payment details.
     * Used by the provider detail page Load button to populate summary cards
     * without fetching the full payment list.
     * Accessible by ADMIN and OPERATOR.
     *
     * @param providerCode the provider code
     * @param from         optional start of date range (inclusive)
     * @param to           optional end of date range (inclusive)
     * @return provider summary with totals only
     */
    @GetMapping("/providers/{providerCode}/summary")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    @Operation(summary = "Provider summary",
            description = "Lightweight totals for a provider within a date range. " +
                    "Used by the provider detail page Load button.")
    public ResponseEntity<ProviderSummaryResponse> getProviderSummary(
            @PathVariable String providerCode,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to) {

        log.info("Provider summary request: providerCode={}, from={}, to={}", providerCode, from, to);

        return ResponseEntity.ok(
                paymentQueryService.getProviderSummary(providerCode, from, to)
        );
    }

    /**
     * Full provider reconciliation report for a given period.
     * Returns totals plus the complete list of all payments for the period.
     * Used exclusively for CSV export — not for UI table display.
     *
     * @param providerCode the provider code
     * @param from         optional start of date range (inclusive)
     * @param to           optional end of date range (inclusive)
     * @return full reconciliation response including all payment details
     */
    @GetMapping("/providers/{providerCode}/reconciliation")
    @Operation(summary = "Provider reconciliation",
            description = "Full settlement report for a provider within a date range. " +
                    "Returns all payments — intended for CSV export only.")
    public ResponseEntity<ProviderReconciliationResponse> getReconciliation(
            @PathVariable String providerCode,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to) {

        log.info("Reconciliation request: providerCode={}, from={}, to={}", providerCode, from, to);

        return ResponseEntity.ok(
                paymentQueryService.getReconciliation(providerCode, from, to)
        );
    }
}