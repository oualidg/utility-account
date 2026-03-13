/*
 * Copyright (c) 2026 Oualid Gharach. All rights reserved.
 *
 * Aiming for production-grade standards through clean code and best practices
 * for educational and informational purposes.
 *
 * Created on: 2/20/2026 at 6:11 PM
 *
 * Feel free to use or contribute. Contact: oualid.gharach@gmail.com
 */
package com.mycompany.api.account.service;

import com.mycompany.api.account.dto.PaymentResponse;
import com.mycompany.api.account.dto.PaymentSummaryResponse;
import com.mycompany.api.account.dto.ProviderBreakdownResponse;
import com.mycompany.api.account.dto.ProviderReconciliationResponse;
import com.mycompany.api.account.dto.ProviderSummaryResponse;
import com.mycompany.api.account.exception.ResourceNotFoundException;
import com.mycompany.api.account.mapper.PaymentMapper;
import com.mycompany.api.account.repository.PaymentProviderRepository;
import com.mycompany.api.account.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Service layer for payment query operations.
 * All methods are read-only — no state is modified.
 *
 * @author Oualid Gharach
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentQueryService {

    private final PaymentRepository paymentRepository;
    private final PaymentProviderRepository paymentProviderRepository;
    private final PaymentMapper paymentMapper;

    /**
     * Search payments with all-optional filters, paginated.
     * Used by the general payment search endpoint.
     *
     * <p>Results are ordered by {@code paymentDate} descending (most recent first).
     * Sort is applied via {@link PageRequest} rather than in JPQL to avoid conflicts
     * with Spring Data pagination.</p>
     *
     * <p><strong>Receipt number search — prefix match only:</strong>
     * The {@code receiptNumber} parameter is passed directly to the repository
     * which applies a prefix LIKE pattern ({@code fragment%}). A leading wildcard
     * would bypass the primary key B-tree index on {@code receipt_number}, causing
     * full table scans at high volumes. Do NOT wrap with {@code %} here or in the
     * repository. See {@link com.mycompany.api.account.repository.PaymentRepository#searchPayments}
     * for full details.</p>
     *
     * @param accountNumber optional account number filter
     * @param customerId    optional customer ID filter
     * @param providerCode  optional provider code filter
     * @param receiptNumber optional receipt number prefix filter
     * @param from          optional start of date range (inclusive)
     * @param to            optional end of date range (inclusive)
     * @param page          zero-based page index
     * @param size          number of records per page
     * @return page of matching payment responses
     */
    @Transactional(readOnly = true)
    public Page<PaymentResponse> searchPayments(Long accountNumber, Long customerId,
                                                String providerCode, String receiptNumber,
                                                Instant from, Instant to,
                                                int page, int size) {
        log.info("Payment search: accountNumber={}, customerId={}, providerCode={}, receiptNumber={}, from={}, to={}, page={}, size={}",
                accountNumber, customerId, providerCode, receiptNumber, from, to, page, size);

        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "paymentDate"));

        return paymentRepository
                .searchPayments(accountNumber, customerId, providerCode, receiptNumber, from, to, pageable)
                .map(paymentMapper::toResponse);
    }

    /**
     * Global payment summary — total volume and count broken down by provider.
     * Used by the dashboard.
     *
     * @param from optional start of date range (inclusive)
     * @param to   optional end of date range (inclusive)
     * @return summary response with totals and per-provider breakdown
     */
    @Transactional(readOnly = true)
    public PaymentSummaryResponse getDashboardSummary(Instant from, Instant to) {
        log.info("Dashboard summary request: from={}, to={}", from, to);

        Object[] totals = paymentRepository.getGlobalTotals(from, to);
        if (totals.length > 0 && totals[0] instanceof Object[]) {
            totals = (Object[]) totals[0];
        }
        BigDecimal totalAmount = totals[0] != null ? (BigDecimal) totals[0] : BigDecimal.ZERO;
        long totalCount = totals[1] != null ? (long) totals[1] : 0L;

        List<ProviderBreakdownResponse> breakdown = paymentRepository
                .getTotalsByProvider(from, to)
                .stream()
                .map(row -> new ProviderBreakdownResponse(
                        (String) row[0],
                        (String) row[1],
                        (BigDecimal) row[2],
                        (long) row[3]
                ))
                .toList();

        return new PaymentSummaryResponse(totalAmount, totalCount, breakdown);
    }

    /**
     * Per-account payment summary — total volume and count for a single account.
     * Used by the account detail page.
     *
     * @param accountNumber the account number to summarise
     * @return summary response with totals and per-provider breakdown
     */
    @Transactional(readOnly = true)
    public PaymentSummaryResponse getAccountSummary(Long accountNumber) {
        log.info("Account summary request: accountNumber={}", accountNumber);

        Object[] totals = paymentRepository.getAccountTotals(accountNumber);
        if (totals.length > 0 && totals[0] instanceof Object[]) {
            totals = (Object[]) totals[0];
        }
        BigDecimal totalAmount = totals[0] != null ? (BigDecimal) totals[0] : BigDecimal.ZERO;
        long totalCount = totals[1] != null ? (long) totals[1] : 0L;

        List<ProviderBreakdownResponse> breakdown = paymentRepository
                .getAccountTotalsByProvider(accountNumber)
                .stream()
                .map(row -> new ProviderBreakdownResponse(
                        (String) row[0],
                        (String) row[1],
                        (BigDecimal) row[2],
                        (long) row[3]
                ))
                .toList();

        return new PaymentSummaryResponse(totalAmount, totalCount, breakdown);
    }

    /**
     * Lightweight provider summary for a given period.
     * Returns only the total transaction count and total amount — no payment details.
     * Used by the provider detail page Load button to populate the totals cards
     * without fetching the full payment list.
     *
     * @param providerCode the provider code
     * @param from         optional start of date range (inclusive)
     * @param to           optional end of date range (inclusive)
     * @return provider summary with totals only
     * @throws ResourceNotFoundException if provider not found
     */
    @Transactional(readOnly = true)
    public ProviderSummaryResponse getProviderSummary(String providerCode, Instant from, Instant to) {
        log.info("Provider summary request: providerCode={}, from={}, to={}", providerCode, from, to);

        var provider = paymentProviderRepository.findByCode(providerCode)
                .orElseThrow(() -> new ResourceNotFoundException("Provider not found: " + providerCode));

        // [FIXED] — use provider-scoped totals, not global totals
        Object[] totals = paymentRepository.getProviderTotals(providerCode, from, to);
        if (totals.length > 0 && totals[0] instanceof Object[]) {
            totals = (Object[]) totals[0];
        }
        BigDecimal totalAmount = totals[0] != null ? (BigDecimal) totals[0] : BigDecimal.ZERO;
        long totalCount = totals[1] != null ? (long) totals[1] : 0L;

        return new ProviderSummaryResponse(
                providerCode,
                provider.getName(),
                totalAmount,
                totalCount
        );
    }

    /**
     * Full provider reconciliation report for a given period.
     * Returns totals plus the complete list of all payments.
     * Used exclusively for CSV export — intentionally unbounded, as the full
     * dataset is required for an accurate reconciliation. Not for UI display.
     *
     * @param providerCode the provider code
     * @param from         optional start of date range (inclusive)
     * @param to           optional end of date range (inclusive)
     * @return full reconciliation response including all payment details
     * @throws ResourceNotFoundException if provider not found
     */
    @Transactional(readOnly = true)
    public ProviderReconciliationResponse getReconciliation(String providerCode,
                                                            Instant from, Instant to) {
        log.info("Reconciliation request: providerCode={}, from={}, to={}", providerCode, from, to);

        var provider = paymentProviderRepository.findByCode(providerCode)
                .orElseThrow(() -> new ResourceNotFoundException("Provider not found: " + providerCode));

        List<PaymentResponse> payments = paymentRepository
                .findByProviderCodeAndDateRange(providerCode, from, to)
                .stream()
                .map(paymentMapper::toResponse)
                .toList();

        BigDecimal totalAmount = payments.stream()
                .map(PaymentResponse::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new ProviderReconciliationResponse(
                providerCode,
                provider.getName(),
                totalAmount,
                payments.size(),
                payments
        );
    }
}