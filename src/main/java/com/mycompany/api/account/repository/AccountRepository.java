/*
 * Copyright (c) 2026 Oualid Gharach. All rights reserved.
 *
 * Aiming for production-grade standards through clean code and best practices
 * for educational and informational purposes.
 *
 * Created on: 2/6/2026 at 9:37 PM
 *
 * Feel free to use or contribute. Contact: oualid.gharach@gmail.com
 */
package com.mycompany.api.account.repository;

import com.mycompany.api.account.entity.Account;
import com.mycompany.api.account.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Account entity.
 * Provides database access methods for account management.
 *
 * @author Oualid Gharach
 */
@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {

    /**
     * Find customer's main account.
     * Used when depositing via customer ID.
     *
     * @param customerId customer ID
     * @return optional containing main account if found
     */
    Optional<Account> findByCustomer_CustomerIdAndMainAccountTrue(Long customerId);

    /**
     * Find all accounts for a customer.
     * Used for displaying customer's account list.
     *
     * @param customerId customer ID
     * @return list of customer's accounts
     */
    List<Account> findByCustomer_CustomerId(Long customerId);

    /**
     * Find account by account number, only if the owning customer is active.
     * Uses explicit JPQL join to avoid reliance on Hibernate proxy initialization
     * and @SQLRestriction behavior for soft-delete checks.
     *
     * @param accountNumber account number
     * @return optional containing account if found and customer is active
     */
    @Query("SELECT a FROM Account a JOIN FETCH a.customer c " +
            "WHERE a.accountNumber = :accountNumber AND c.active = true")
    Optional<Account> findActiveByAccountNumber(@Param("accountNumber") Long accountNumber);

    /**
     * Atomically update account balance.
     * Thread-safe operation that prevents lost updates in concurrent scenarios.
     * Uses database-level atomic UPDATE to add amount to current balance.
     *
     * @param accountNumber account number
     * @param amount amount to add (can be negative for withdrawals)
     * @return number of rows updated (1 if successful, 0 if account not found)
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Account a SET a.balance = a.balance + :amount, a.updatedAt = CAST(CURRENT_TIMESTAMP AS Instant) " +
            "WHERE a.accountNumber = :accountNumber")
    int updateBalanceAtomic(@Param("accountNumber") Long accountNumber, @Param("amount") BigDecimal amount);

    /**
     * Check if a main account already exists for a specific customer entity.
     * Used to enforce the business rule of one main account per customer.
     *
     * @param customer customer entity
     * @return true if a main account exists, false otherwise
     */
    boolean existsByCustomerAndMainAccountTrue(Customer customer);
}