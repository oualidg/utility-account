/*
 * Copyright (c) 2026 Oualid Gharach. All rights reserved.
 *
 * Aiming for production-grade standards through clean code and best practices
 * for educational and informational purposes.
 *
 * Created on: 2/7/2026 at 1:23 PM
 *
 * Feel free to use or contribute. Contact: oualid.gharach@gmail.com
 */
package com.mycompany.api.account.service;

import com.mycompany.api.account.dto.DepositRequest;
import com.mycompany.api.account.entity.Payment;
import com.mycompany.api.account.entity.PaymentProvider;
import com.mycompany.api.account.exception.DuplicateResourceException;
import com.mycompany.api.account.exception.ResourceNotFoundException;
import com.mycompany.api.account.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service layer for Payment operations.
 * Handles payment processing with idempotency.
 *
 * <p>Concurrency strategy: deposits are processed "insert-first" — the payment
 * is inserted before the balance is updated, relying on the database unique
 * constraint (uk_payment_provider_reference) to prevent duplicates atomically.
 * If a concurrent request causes a constraint violation, the transaction rolls
 * back (no balance change) and the existing payment is returned.</p>
 *
 * <p>This class is intentionally non-transactional for deposit methods. It delegates
 * transactional work to {@link PaymentTransactionHelper} and catches
 * DataIntegrityViolationException outside the rolled-back transaction boundary,
 * allowing the duplicate lookup to run in a fresh persistence context.</p>
 *
 * @author Oualid Gharach
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentTransactionHelper transactionHelper;

    private final PaymentRepository paymentRepository;

    private final AccountService accountService;

    /**
     * Deposit to customer's main account by customer ID.
     * Idempotent: if same provider+reference already exists, returns the existing payment.
     *
     * @param customerId customer ID
     * @param request deposit request
     * @param provider authenticated payment provider
     * @return payment entity (new or existing)
     */
    public Payment depositToMainAccount(Long customerId, DepositRequest request,
                                        PaymentProvider provider) {
        try {
            return transactionHelper.executeDeposit(request, provider,
                    () -> accountService.getMainAccount(customerId),
                    "customerId=" + customerId);
        } catch (DataIntegrityViolationException e) {
            log.debug("Constraint violation caught, looking up existing payment: {}", e.getMessage());
            // Resolve the main account number for the idempotency mismatch check.
            Long mainAccountNumber = accountService.getMainAccount(customerId).getAccountNumber();
            return handleDuplicatePayment(request, provider, mainAccountNumber, "customerId=" + customerId);
        }
    }

    /**
     * Deposit to specific account by account number.
     * Idempotent: if same provider+reference already exists, returns the existing payment.
     *
     * @param accountNumber account number
     * @param request deposit request
     * @param provider authenticated payment provider
     * @return payment entity (new or existing)
     */
    public Payment depositToAccount(Long accountNumber, DepositRequest request,
                                    PaymentProvider provider) {
        try {
            return transactionHelper.executeDeposit(request, provider,
                    () -> accountService.getAccount(accountNumber),
                    "accountNumber=" + accountNumber);
        } catch (DataIntegrityViolationException e) {
            log.debug("Constraint violation caught, looking up existing payment: {}", e.getMessage());
            return handleDuplicatePayment(request, provider, accountNumber, "accountNumber=" + accountNumber);
        }
    }

    /**
     * Get payment by provider and reference (for confirmation).
     *
     * @param provider authenticated payment provider
     * @param reference payment reference
     * @return payment entity if found
     */
    @Transactional(readOnly = true)
    public Payment getPaymentByReference(PaymentProvider provider, String reference) {
        return paymentRepository
                .findByProviderAndReferenceWithDetails(provider, reference)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Payment not found with provider " + provider.getCode() + " and reference " + reference
                ));
    }

    /**
     * Handle a duplicate payment detected via unique constraint violation.
     * Called outside the rolled-back transaction so a fresh persistence context is used.
     *
     * <p>Validates that the incoming request matches the existing payment's amount and
     * target account. A mismatched duplicate (same provider+reference but different
     * amount or account) is rejected — this indicates a bug or suspicious request,
     * not a legitimate retry.</p>
     *
     * @param request the incoming deposit request
     * @param provider the authenticated payment provider
     * @param targetAccountNumber the account number the caller intended to deposit to
     * @param logContext contextual info for log messages
     * @return the existing payment if it matches the request
     * @throws DuplicateResourceException if the request doesn't match the existing payment
     */
    private Payment handleDuplicatePayment(DepositRequest request, PaymentProvider provider,
                                           Long targetAccountNumber, String logContext) {

        log.info("Duplicate payment detected for {}, reference={}", logContext, request.paymentReference());

        Payment existing = transactionHelper
                .findByProviderAndReference(provider, request.paymentReference())
                .orElseThrow(() -> {
                    log.error("Constraint violation confirmed duplicate but lookup failed: provider={}, reference={}",
                            provider.getCode(), request.paymentReference());
                    return new DuplicateResourceException(
                            "Payment with reference " + request.paymentReference()
                                    + " already exists. Use the confirmation endpoint to retrieve it."
                    );
                });

        // Validate idempotency: a legitimate retry must match amount and account
        if (existing.getAmount().compareTo(request.amount()) != 0
                || !existing.getAccount().getAccountNumber().equals(targetAccountNumber)) {

            log.warn("Idempotency mismatch for reference={}: existing [account={}, amount={}], "
                            + "requested [account={}, amount={}]",
                    request.paymentReference(),
                    existing.getAccount().getAccountNumber(), existing.getAmount(),
                    targetAccountNumber, request.amount());

            throw new DuplicateResourceException(
                    "Payment reference " + request.paymentReference()
                            + " already exists with different details. "
                            + "Use a unique reference for each distinct payment."
            );
        }

        return existing;
    }

}