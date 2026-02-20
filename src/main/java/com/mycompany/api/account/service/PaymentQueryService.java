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
import com.mycompany.api.account.exception.ResourceNotFoundException;
import com.mycompany.api.account.mapper.PaymentMapper;
import com.mycompany.api.account.repository.PaymentProviderRepository;
import com.mycompany.api.account.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentQueryService {

    private final PaymentRepository paymentRepository;
    private final PaymentProviderRepository paymentProviderRepository;
    private final PaymentMapper paymentMapper;

    @Transactional(readOnly = true)
    public List<PaymentResponse> searchPayments(Long accountNumber, Long customerId,
                                                String providerCode, Instant from, Instant to) {
        log.info("Payment search: accountNumber={}, customerId={}, providerCode={}, from={}, to={}",
                accountNumber, customerId, providerCode, from, to);

        return paymentRepository
                .searchPayments(accountNumber, customerId, providerCode, from, to)
                .stream()
                .map(paymentMapper::toResponse)
                .toList();
    }

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