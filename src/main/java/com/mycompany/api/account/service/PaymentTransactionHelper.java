/*
 * Copyright (c) 2026 Oualid Gharach. All rights reserved.
 *
 * Aiming for production-grade standards through clean code and best practices
 * for educational and informational purposes.
 *
 * Created on: 2/13/2026 at 9:13 PM
 *
 * Feel free to use or contribute. Contact: oualid.gharach@gmail.com
 */
package com.mycompany.api.account.service;

import com.mycompany.api.account.dto.DepositRequest;
import com.mycompany.api.account.entity.Account;
import com.mycompany.api.account.entity.Payment;
import com.mycompany.api.account.model.PaymentProvider;
import com.mycompany.api.account.repository.PaymentRepository;
import com.mycompany.api.account.util.UuidGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * Transactional helper for payment operations.
 *
 * <p>Extracted into a separate bean so that Spring's proxy-based @Transactional
 * is properly intercepted. The parent {@link PaymentService} delegates here
 * and handles DataIntegrityViolationException outside the transaction boundary.</p>
 *
 * @author Oualid Gharach
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class PaymentTransactionHelper {

    private final PaymentRepository paymentRepository;

    private final AccountService accountService;

    private final UuidGenerator uuidGenerator;

    /**
     * Execute a deposit within a single transaction.
     *
     * <p>Order of operations:</p>
     * <ol>
     *   <li>Resolve and validate account (ensures customer is active)</li>
     *   <li>INSERT payment via saveAndFlush — DB unique constraint prevents duplicates</li>
     *   <li>UPDATE balance atomically — only reached if INSERT succeeded</li>
     * </ol>
     *
     * <p>If the INSERT violates the unique constraint (uk_payment_provider_reference),
     * the entire transaction rolls back (no balance change), and the
     * DataIntegrityViolationException propagates to the caller.</p>
     *
     * @param request deposit request
     * @param accountLookup supplier to resolve the target account
     * @param logContext contextual info for log messages
     * @return the persisted payment entity
     */
    @Transactional
    public Payment executeDeposit(DepositRequest request, Supplier<Account> accountLookup, String logContext) {

        log.info("Processing deposit {}, provider={}, reference={}, amount={}",
                logContext, request.paymentProvider(), request.paymentReference(), request.amount());

        // 1. Resolve account and validate customer is active
        Account account = accountLookup.get();

        // 2. Insert payment — saveAndFlush forces immediate INSERT,
        //    triggering the unique constraint check before we update the balance
        Payment payment = createPayment(account, request);

        // 3. Update balance atomically (only reached if INSERT succeeded)
        accountService.updateBalance(account, request.amount());

        log.info("Payment successful: receipt={}, {}, amount={}",
                payment.getReceiptNumber(), logContext, request.amount());

        return payment;
    }

    /**
     * Look up an existing payment by provider and reference.
     * Runs in its own read-only transaction (fresh persistence context after rollback).
     *
     * @param provider payment provider
     * @param reference payment reference
     * @return the existing payment if found
     */
    @Transactional(readOnly = true)
    public Optional<Payment> findByProviderAndReference(PaymentProvider provider, String reference) {
        return paymentRepository.findByPaymentProviderAndPaymentReference(provider, reference);
    }

    /**
     * Create payment entity and persist immediately.
     */
    private Payment createPayment(Account account, DepositRequest request) {
        String receiptNumber = uuidGenerator.generate();

        Payment payment = new Payment();
        payment.setReceiptNumber(receiptNumber);
        payment.setAccount(account);
        payment.setAmount(request.amount());
        payment.setPaymentProvider(request.paymentProvider());
        payment.setPaymentReference(request.paymentReference());
        // paymentDate set by @PrePersist

        // saveAndFlush forces immediate INSERT, ensuring the unique constraint
        // is checked within this transaction before we proceed to update balance
        return paymentRepository.saveAndFlush(payment);
    }
}