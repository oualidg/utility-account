/*
 * Copyright (c) 2026 Oualid Gharach. All rights reserved.
 *
 * Aiming for production-grade standards through clean code and best practices
 * for educational and informational purposes.
 *
 * Created on: 2/3/2026 at 8:43 PM
 *
 * Feel free to use or contribute. Contact: oualid.gharach@gmail.com
 */
package com.mycompany.api.account.dto;

import java.time.Instant;

/**
 * Response when returning customer data.
 *
 * @author Oualid Gharach
 */
public record CustomerResponse(
        Long customerId,
        String firstName,
        String lastName,
        String email,
        String mobileNumber,
        Instant createdAt,
        Instant updatedAt
) {
}