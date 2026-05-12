/*
 * Copyright (c) 2026 Oualid Gharach. All rights reserved.
 *
 * Aiming for production-grade standards through clean code and best practices
 * for educational and informational purposes.
 *
 * Created on: 2/7/2026 at 1:20 PM
 *
 * Feel free to use or contribute. Contact: oualid.gharach@gmail.com
 */
package com.mycompany.api.account.controller;

import com.mycompany.api.account.dto.DepositRequest;
import com.mycompany.api.account.dto.PaymentResponse;
import com.mycompany.api.account.entity.Payment;
import com.mycompany.api.account.entity.PaymentProvider;
import com.mycompany.api.account.filter.ApiKeyAuthFilter;
import com.mycompany.api.account.mapper.PaymentMapper;
import com.mycompany.api.account.service.PaymentService;
import com.mycompany.api.account.service.PaymentValidationService;
import com.mycompany.api.account.validation.ValidLuhn;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for payment operations.
 * Handles deposit transactions, payment confirmations, and pre-payment validation.
 *
 * <p>All endpoints require API key authentication via the {@code X-Api-Key} header.
 * The authenticated provider is resolved by {@link ApiKeyAuthFilter} and stored
 * as a request attribute.</p>
 *
 * <p>Validation endpoints return HTTP status only — no response body. This is
 * intentional, callers receive enough information to make a decision
 * without exposing billing data.</p>
 *
 * @author Oualid Gharach
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Payments", description = "Payment transaction management")
@Slf4j
public class PaymentController {

    private final PaymentService paymentService;

    private final PaymentMapper paymentMapper;

    private final PaymentValidationService paymentValidationService;

    /**
     * Deposit money to a specific account.
     *
     * @param accountNumber account number (10-digit Luhn)
     * @param request deposit request
     * @param httpRequest servlet request containing authenticated provider
     * @return payment response with receipt
     */
    @PostMapping("/accounts/{accountNumber}/payments")
    @Operation(summary = "Deposit to account", description = "Deposit money to a specific account by account number")
    public ResponseEntity<PaymentResponse> depositToAccount(
            @PathVariable @ValidLuhn(length = 10, message = "Account number must be a valid 10-digit number with checksum")
            Long accountNumber,
            @Valid @RequestBody DepositRequest request,
            HttpServletRequest httpRequest) {

        PaymentProvider provider = getAuthenticatedProvider(httpRequest);

        log.info("Deposit to account request received: accountNumber={}, provider={}, reference={}, amount={}",
                accountNumber, provider.getCode(), request.paymentReference(), request.amount());

        Payment payment = paymentService.depositToAccount(accountNumber, request, provider);

        log.info("Deposit successful: receipt={}, accountNumber={}",
                payment.getReceiptNumber(), accountNumber);

        return ResponseEntity.ok(paymentMapper.toResponse(payment));
    }

    /**
     * Deposit money to customer's main account.
     *
     * @param customerId customer ID (8-digit Luhn)
     * @param request deposit request
     * @param httpRequest servlet request containing authenticated provider
     * @return payment response with receipt
     */
    @PostMapping("/customers/{customerId}/payments")
    @Operation(summary = "Deposit to main account", description = "Deposit money to customer's main account by customer ID")
    public ResponseEntity<PaymentResponse> depositToMainAccount(
            @PathVariable @ValidLuhn(length = 8, message = "Customer ID must be a valid 8-digit number with checksum")
            Long customerId,
            @Valid @RequestBody DepositRequest request,
            HttpServletRequest httpRequest) {

        PaymentProvider provider = getAuthenticatedProvider(httpRequest);

        log.info("Deposit to main account request received: customerId={}, provider={}, reference={}, amount={}",
                customerId, provider.getCode(), request.paymentReference(), request.amount());

        Payment payment = paymentService.depositToMainAccount(customerId, request, provider);

        log.info("Deposit to main account successful: receipt={}, customerId={}",
                payment.getReceiptNumber(), customerId);

        return ResponseEntity.ok(paymentMapper.toResponse(payment));
    }

    /**
     * Confirm payment by reference.
     * Provider is identified by the API key.
     *
     * @param reference payment reference
     * @param httpRequest servlet request containing authenticated provider
     * @return payment response if found, 404 if not found
     */
    @GetMapping("/payments/confirmation/{reference}")
    @Operation(summary = "Confirm payment", description = "Check payment status by reference")
    public ResponseEntity<PaymentResponse> confirmPayment(
            @PathVariable String reference,
            HttpServletRequest httpRequest) {

        PaymentProvider provider = getAuthenticatedProvider(httpRequest);

        log.info("Payment confirmation request: provider={}, reference={}", provider.getCode(), reference);

        Payment payment = paymentService.getPaymentByReference(provider, reference);

        log.info("Payment found: receipt={}, provider={}, reference={}",
                payment.getReceiptNumber(), provider.getCode(), reference);

        return ResponseEntity.ok(paymentMapper.toResponse(payment));
    }

    /**
     * Validate that an account exists and is eligible to receive a payment.
     *
     * <p>Used by external payment gateway integrations during pre-payment validation
     * flows — before funds move. Response is HTTP status only, per ADR-002.</p>
     *
     * <ul>
     *   <li>200 — account is valid and active</li>
     *   <li>404 — account not found or owning customer is inactive</li>
     * </ul>
     *
     * @param accountNumber account number (10-digit Luhn)
     * @param httpRequest servlet request containing authenticated provider
     * @return 200 if valid, 404 if not found
     */
    @GetMapping("/accounts/{accountNumber}/validate")
    @Operation(summary = "Validate account", description = "Check if an account exists and is eligible to receive a payment. Returns HTTP status only.")
    public ResponseEntity<Void> validateAccount(
            @PathVariable @ValidLuhn(length = 10, message = "Account number must be a valid 10-digit number with checksum")
            Long accountNumber,
            HttpServletRequest httpRequest) {

        PaymentProvider provider = getAuthenticatedProvider(httpRequest);

        log.info("Account validation request: accountNumber={}, provider={}", accountNumber, provider.getCode());

        if (!paymentValidationService.isAccountValid(accountNumber)) {
            log.warn("Account validation failed: accountNumber={}, provider={}", accountNumber, provider.getCode());
            return ResponseEntity.notFound().build();
        }

        log.info("Account validation passed: accountNumber={}, provider={}", accountNumber, provider.getCode());
        return ResponseEntity.ok().build();
    }

    /**
     * Validate that a customer exists and has a main account eligible to receive a payment.
     *
     * <p>Used by external payment gateway integrations during pre-payment validation
     * flows — before funds move. Response is HTTP status only, per ADR-002.</p>
     *
     * <ul>
     *   <li>200 — customer is active and has a main account</li>
     *   <li>404 — customer not found, inactive, or has no main account</li>
     * </ul>
     *
     * @param customerId customer ID (8-digit Luhn)
     * @param httpRequest servlet request containing authenticated provider
     * @return 200 if valid, 404 if not found
     */
    @GetMapping("/customers/{customerId}/validate")
    @Operation(summary = "Validate customer", description = "Check if a customer is active and has a main account eligible to receive a payment. Returns HTTP status only.")
    public ResponseEntity<Void> validateCustomer(
            @PathVariable @ValidLuhn(length = 8, message = "Customer ID must be a valid 8-digit number with checksum")
            Long customerId,
            HttpServletRequest httpRequest) {

        PaymentProvider provider = getAuthenticatedProvider(httpRequest);

        log.info("Customer validation request: customerId={}, provider={}", customerId, provider.getCode());

        if (!paymentValidationService.isCustomerValid(customerId)) {
            log.warn("Customer validation failed: customerId={}, provider={}", customerId, provider.getCode());
            return ResponseEntity.notFound().build();
        }

        log.info("Customer validation passed: customerId={}, provider={}", customerId, provider.getCode());
        return ResponseEntity.ok().build();
    }

    /**
     * Extract the authenticated provider from the request attribute set by ApiKeyAuthFilter.
     *
     * <p>The provider is guaranteed to be present if {@link ApiKeyAuthFilter} ran correctly —
     * it sets the attribute only after successful API key authentication. A null value here
     * indicates a filter misconfiguration, not a business error.</p>
     *
     * @throws IllegalStateException if the provider attribute is missing — signals a wiring error
     */
    private PaymentProvider getAuthenticatedProvider(HttpServletRequest request) {
        PaymentProvider provider = (PaymentProvider) request.getAttribute(ApiKeyAuthFilter.PROVIDER_ATTRIBUTE);
        if (provider == null) {
            log.error("No authenticated provider on request {} {} — ApiKeyAuthFilter may have been bypassed",
                    request.getMethod(), request.getRequestURI());
            throw new IllegalStateException("No authenticated provider found on request");
        }
        return provider;
    }
}