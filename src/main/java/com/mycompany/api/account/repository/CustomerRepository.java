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
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
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

    @EntityGraph(attributePaths = {"accounts"}) // Fetches accounts in the same query
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
     * Find customers whose mobile number contains the given string.
     * Case-insensitive search.
     *
     * @param mobileNumber the mobile number string to search for
     * @return list of customers with matching mobile numbers
     */
    List<Customer> findByMobileNumberContaining(String mobileNumber);
}