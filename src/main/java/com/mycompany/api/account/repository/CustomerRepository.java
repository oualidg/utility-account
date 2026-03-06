/*
 * Copyright (c) 2026 Oualid Gharach. All rights reserved.
 *
 * Aiming for production-grade standards through clean code and best practices
 * for educational and informational purposes.
 *
 * Created on: 2/3/2026 at 9:31 PM
 *
 * Feel free to use or contribute. Contact: oualid.gharach@gmail.com
 */
package com.mycompany.api.account.repository;

import com.mycompany.api.account.entity.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository interface for Customer entity.
 * Provides database operations for customers.
 * Spring Data JPA automatically implements this interface at runtime.
 *
 * @author Oualid Gharach
 */
@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {

    /**
     * Find customer by ID with accounts eagerly fetched in a single query.
     * Used for detail view where accounts are always needed.
     *
     * @param id the customer ID
     * @return optional containing customer with accounts if found
     */
    @EntityGraph(attributePaths = {"accounts"})
    @Query("SELECT c FROM Customer c WHERE c.customerId = :id")
    Optional<Customer> findByIdWithAccounts(@Param("id") Long id);

    /**
     * Check if ANY customer (active or soft-deleted) exists with the given email.
     * Bypasses @SQLRestriction with a native query to see all rows.
     * Used to give accurate error messages when email conflicts with a soft-deleted customer.
     *
     * @param email the email address
     * @return true if any customer (active or inactive) has this email
     */
    @Query(value = "SELECT COUNT(*) > 0 FROM customers WHERE email = :email", nativeQuery = true)
    boolean existsByEmailIncludingInactive(@Param("email") String email);

    /**
     * Search customers by mobile number fragment.
     *
     * <p><strong>Performance note:</strong>
     * This method uses {@code LIKE '%fragment%'} which cannot use the B-tree index
     * on {@code mobile_number}. A prefix match ({@code LIKE 'fragment%'}) would use
     * the index but requires mobile numbers to be stored in a consistent format.
     * Currently both {@code 0821234567} and {@code +27821234567} formats exist in
     * the database. Prerequisite for optimisation: normalise all mobile numbers to
     * E.164 format ({@code +27821234567}) at write time in CustomerMapper, then
     * migrate existing data, then switch to {@code findByMobileNumberStartingWith}.</p>
     *
     * @param mobileNumber the mobile number fragment to search for
     * @param pageable     pagination and sort parameters
     * @return page of matching customers
     */
    Page<Customer> findByMobileNumberContaining(String mobileNumber, Pageable pageable);

    /**
     * Search customers by last name fragment (case-insensitive contains).
     *
     * <p><strong>Performance note — future scalability:</strong>
     * This method uses {@code LIKE '%fragment%'} which cannot use a standard B-tree
     * index. At current scale this is acceptable. At 1M+ customers, add a
     * database-appropriate full-text or trigram index:
     * <ul>
     *   <li>PostgreSQL: {@code pg_trgm} GIN index —
     *       {@code CREATE INDEX idx_customers_last_name_trgm ON customers
     *       USING GIN (LOWER(last_name) gin_trgm_ops);}</li>
     *   <li>Oracle: Oracle Text CONTEXT index with {@code CONTAINS()}</li>
     *   <li>SQL Server: Full-text index with {@code CONTAINS()}</li>
     * </ul>
     * No query or method signature change needed — only the index addition.</p>
     *
     * @param surname  the surname fragment to search for
     * @param pageable pagination and sort parameters
     * @return page of matching customers
     */
    Page<Customer> findByLastNameContainingIgnoreCase(String surname, Pageable pageable);
}