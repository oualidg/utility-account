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
import com.mycompany.api.account.mapper.PaymentMapper;
import com.mycompany.api.account.model.PaymentProvider;
import com.mycompany.api.account.service.PaymentService;
import com.mycompany.api.account.validation.ValidLuhn;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for payment operations.
 * Handles deposit transactions and payment confirmations.
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

    /**
     * Deposit money to a specific account.
     *
     * @param accountNumber account number (10-digit Luhn)
     * @param request deposit request
     * @return payment response with receipt
     */
    @PostMapping("/accounts/{accountNumber}/payments")
    @Operation(summary = "Deposit to account", description = "Deposit money to a specific account by account number")
    public ResponseEntity<PaymentResponse> depositToAccount(
            @PathVariable @ValidLuhn(length = 10, message = "Account number must be a valid 10-digit number with checksum")
            Long accountNumber,
            @Valid @RequestBody DepositRequest request) {

        log.info("Deposit to account request received: accountNumber={}, provider={}, reference={}, amount={}",
                accountNumber, request.paymentProvider(), request.paymentReference(), request.amount());

        Payment payment = paymentService.depositToAccount(accountNumber, request);

        log.info("Deposit successful: receipt={}, accountNumber={}",
                payment.getReceiptNumber(), accountNumber);

        return ResponseEntity.ok(paymentMapper.toResponse(payment));
    }

    /**
     * Deposit money to customer's main account.
     *
     * @param customerId customer ID (8-digit Luhn)
     * @param request deposit request
     * @return payment response with receipt
     */
    @PostMapping("/customers/{customerId}/payments")
    @Operation(summary = "Deposit to main account", description = "Deposit money to customer's main account by customer ID")
    public ResponseEntity<PaymentResponse> depositToMainAccount(
            @PathVariable @ValidLuhn(length = 8, message = "Customer ID must be a valid 8-digit number with checksum")
            Long customerId,
            @Valid @RequestBody DepositRequest request) {

        log.info("Deposit to main account request received: customerId={}, provider={}, reference={}, amount={}",
                customerId, request.paymentProvider(), request.paymentReference(), request.amount());

        Payment payment = paymentService.depositToMainAccount(customerId, request);

        log.info("Deposit to main account successful: receipt={}, customerId={}",
                payment.getReceiptNumber(), customerId);

        return ResponseEntity.ok(paymentMapper.toResponse(payment));
    }

    /**
     * Confirm payment by provider and reference.
     * Used to check if payment was processed (e.g., after timeout).
     *
     * @param provider payment provider
     * @param reference payment reference
     * @return payment response if found, 404 if not found
     * @throws IllegalArgumentException if provider is invalid (caught by GlobalExceptionHandler)
     */
    @GetMapping("/payments/confirmation/{provider}/{reference}")
    @Operation(summary = "Confirm payment", description = "Check payment status by provider and reference")
    public ResponseEntity<PaymentResponse> confirmPayment(
            @PathVariable("provider") PaymentProvider provider,
            @PathVariable String reference) {

        log.info("Payment confirmation request: provider={}, reference={}", provider, reference);

        Payment payment = paymentService.getPaymentByReference(provider, reference);

        log.info("Payment found: receipt={}, provider={}, reference={}",
                payment.getReceiptNumber(), provider, reference);

        return ResponseEntity.ok(paymentMapper.toResponse(payment));
    }

}