/*
 * Copyright (c) 2026 Oualid Gharach. All rights reserved.
 *
 * Aiming for production-grade standards through clean code and best practices
 * for educational and informational purposes.
 *
 * Created on: 2/6/2026 at 7:24 PM
 *
 * Feel free to use or contribute. Contact: oualid.gharach@gmail.com
 */
package com.mycompany.api.account.dto;

/**
 * Summary view of a customer.
 * Used when returning customer lists
 *
 * @author Oualid Gharach
 */
public record CustomerSummaryResponse(
        Long customerId,
        String firstName,
        String lastName,
        String email,
        String mobileNumber
) {}