/*
 * Copyright (c) 2026 Oualid Gharach. All rights reserved.
 *
 * Aiming for production-grade standards through clean code and best practices
 * for educational and informational purposes.
 *
 * Created on: 2/7/2026 at 12:13 PM
 *
 * Feel free to use or contribute. Contact: oualid.gharach@gmail.com
 */
package com.mycompany.api.account.service;

import com.mycompany.api.account.entity.Account;
import com.mycompany.api.account.entity.Customer;
import com.mycompany.api.account.exception.BalanceUpdateException;
import com.mycompany.api.account.exception.DuplicateResourceException;
import com.mycompany.api.account.exception.ResourceNotFoundException;
import com.mycompany.api.account.repository.AccountRepository;
import com.mycompany.api.account.util.LuhnGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Service layer for Account operations.
 * Handles account creation with Luhn collision retry.
 *
 * @author Oualid Gharach
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final LuhnGenerator luhnGenerator;

    /**
     * Create main account for a customer.
     * Called automatically when customer is created during onboarding.
     *
     * @param customer customer entity (already persisted)
     * @return created account entity
     */
    @Transactional
    public Account createMainAccount(Customer customer) {
        log.info("Creating main account for customer: {}", customer.getCustomerId());

        boolean hasMain = accountRepository.existsByCustomerAndMainAccountTrue(customer);

        if (hasMain) {
            throw new DuplicateResourceException(
                    "Customer " + customer.getCustomerId() + " already has a main account"
            );
        }

        Account saved = createAccount(customer, true);

        log.info("Main account created successfully: accountNumber={}, customerId={}",
                saved.getAccountNumber(), customer.getCustomerId());

        return saved;
    }

    /**
     * Create a standard (non-main) account for a customer.
     * This can be used for secondary utility services or additional sub-accounts.
     *
     * @param customer customer entity (already persisted)
     */
    public void createAccount(Customer customer) {
        log.info("Creating account for customer: {}", customer.getCustomerId());

        Account saved = createAccount(customer, false);

        log.info("Account created successfully: accountNumber={}, customerId={}",
                saved.getAccountNumber(), customer.getCustomerId());

    }

    /**
     * Internal helper method to handle the common logic for account creation.
     * Encapsulates Luhn number generation and default balance initialization.
     *
     * @param customer the customer to whom the account belongs
     * @param isMain flag indicating if this is the primary account
     * @return the persisted account entity
     */
    private Account createAccount(Customer customer, boolean isMain) {

        // Generate 10-digit Luhn account number
        Long accountNumber = luhnGenerator.generateAccountNumber();

        Account account = new Account();
        account.setCustomer(customer);
        account.setAccountNumber(accountNumber);
        account.setBalance(BigDecimal.ZERO);
        account.setMainAccount(isMain);
        // Timestamps set by @PrePersist

        return accountRepository.save(account);
    }

    /**
     * Get customer's main account.
     * Used for deposits via customer ID.
     *
     * @param customerId customer ID
     * @return main account entity
     */
    @Transactional(readOnly = true)
    public Account getMainAccount(Long customerId) {
        return accountRepository.findByCustomer_CustomerIdAndMainAccountTrue(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Main account not found for customer: " + customerId));
    }

    /**
     * Get account by account number.
     * Only returns accounts whose owning customer is active.
     *
     * @param accountNumber account number
     * @return account entity with customer eagerly loaded
     * @throws ResourceNotFoundException if account not found or owner is inactive
     */
    @Transactional(readOnly = true)
    public Account getAccount(Long accountNumber) {
        return accountRepository.findActiveByAccountNumber(accountNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + accountNumber));
    }

    /**
     * Get all accounts for a customer.
     *
     * @param customerId customer ID
     * @return list of account summaries
     */
    @Transactional(readOnly = true)
    public List<Account> getCustomerAccounts(Long customerId) {
        return accountRepository.findByCustomer_CustomerId(customerId);  // ✅ Return entities
    }


    /**
     * Update account balance atomically.
     * Uses database-level atomic update to prevent race conditions.
     * This ensures that concurrent deposits to the same account don't result in lost updates.
     *
     * @param account account to update
     * @param amount amount to add to balance
     */
    @Transactional
    public void updateBalance(Account account, BigDecimal amount) {
        try {
            int updated = accountRepository.updateBalanceAtomic(
                    account.getAccountNumber(),
                    amount
            );

            if (updated == 0) {
                log.error("Failed to update balance - account may have been deleted: accountNumber={}",
                        account.getAccountNumber());
                throw new ResourceNotFoundException(
                        "Unable to update account balance - account not found or inactive: " +
                                account.getAccountNumber()
                );
            }

            log.info("Account balance updated atomically: accountNumber={}, amount={}",
                    account.getAccountNumber(), amount);

        } catch (DataIntegrityViolationException e) {
            log.warn("Balance update failed for accountNumber={}, amount={}: {}",
                    account.getAccountNumber(), amount, e.getMostSpecificCause().getMessage());
            throw new BalanceUpdateException(
                    "Balance update failed for account: " + account.getAccountNumber()
            );
        }
    }
}