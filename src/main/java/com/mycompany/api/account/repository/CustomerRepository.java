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
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Customer entity.
 * Provides database operations for customers.
 *
 * Spring Data JPA automatically implements this interface at runtime.
 *
 * @author Oualid Gharach
 */
@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {

    /**
     * Check if a customer exists with the given email.
     *
     * @param email the email address
     * @return true if customer exists with this email
     */
    boolean existsByEmail(String email);

    /**
     * Find customers whose mobile number contains the given string.
     * Case-insensitive search.
     *
     * @param mobileNumber the mobile number string to search for
     * @return list of customers with matching mobile numbers
     */
    List<Customer> findByMobileNumberContaining(String mobileNumber);
}