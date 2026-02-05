/*
 * Copyright (c) 2026 Oualid Gharach. All rights reserved.
 *
 * Aiming for production-grade standards through clean code and best practices
 * for educational and informational purposes.
 *
 * Created on: 2/3/2026 at 4:49 PM
 *
 * Feel free to use or contribute. Contact: oualid.gharach@gmail.com
 */
package com.mycompany.api.account;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;

/**
 * Main Spring Boot application class for Utility Account API.
 *
 * @EnableRetry - Enables Spring Retry for handling transient failures (e.g., ID collisions)
 *
 * Note: Timestamp auditing uses JPA lifecycle callbacks (@PrePersist, @PreUpdate) in entities
 *
 * @author Oualid Gharach
 */
@SpringBootApplication
@EnableRetry
public class UtilityAccountApplication {

    public static void main(String[] args) {
        SpringApplication.run(UtilityAccountApplication.class, args);
    }

}
