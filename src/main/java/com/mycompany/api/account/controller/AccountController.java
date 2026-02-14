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

import com.mycompany.api.account.dto.AccountResponse;
import com.mycompany.api.account.dto.AccountSummaryResponse;
import com.mycompany.api.account.entity.Account;
import com.mycompany.api.account.mapper.AccountMapper;
import com.mycompany.api.account.service.AccountService;
import com.mycompany.api.account.validation.ValidLuhn;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for account operations.
 * Minimal implementation for Phase 2 (viewing accounts only).
 *
 * @author Oualid Gharach
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Accounts", description = "Account management")
@Slf4j
public class AccountController {

    private final AccountService accountService;

    private final AccountMapper accountMapper;

    /**
     * Get all accounts for a customer.
     *
     * @param customerId customer ID (8-digit Luhn)
     * @return list of customer's accounts
     */
    @GetMapping("/customers/{customerId}/accounts")
    @Operation(summary = "Get customer accounts", description = "Retrieve all accounts for a specific customer")
    public ResponseEntity<List<AccountSummaryResponse>> getCustomerAccounts(
            @PathVariable @ValidLuhn(length = 8, message = "Customer ID must be a valid 8-digit number with checksum")
            Long customerId) {

        log.info("Get accounts request for customer: {}", customerId);

        List<Account> accounts = accountService.getCustomerAccounts(customerId);

        List<AccountSummaryResponse> response = accounts.stream()
                .map(accountMapper::toSummaryResponse)
                .toList();

        log.info("Returning {} accounts for customer: {}", accounts.size(), customerId);

        return ResponseEntity.ok(response);
    }

    /**
     * Get account by account number.
     * Used for validation by M-Pesa microservice and account lookup.
     *
     * @param accountNumber account number (10-digit Luhn)
     * @return full account details
     */
    @GetMapping("/accounts/{accountNumber}")
    @Operation(summary = "Get account", description = "Retrieve account details by account number")
    public ResponseEntity<AccountResponse> getAccount(
            @PathVariable @ValidLuhn(length = 10, message = "Account number must be a valid 10-digit number with checksum")
            Long accountNumber) {

        log.info("Get account request: accountNumber={}", accountNumber);

        Account account = accountService.getAccount(accountNumber);
        AccountResponse response = accountMapper.toResponse(account);

        log.info("Account found: accountNumber={}, customerId={}",
                accountNumber, response.customerId());

        return ResponseEntity.ok(response);
    }

}
