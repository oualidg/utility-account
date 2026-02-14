/*
 * Copyright (c) 2026 Oualid Gharach. All rights reserved.
 *
 * Aiming for production-grade standards through clean code and best practices
 * for educational and informational purposes.
 *
 * Created on: 2/6/2026 at 9:26 PM
 *
 * Feel free to use or contribute. Contact: oualid.gharach@gmail.com
 */
package com.mycompany.api.account.util;

import com.github.f4b6a3.uuid.UuidCreator;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Generates UUID v7 identifiers for various purposes.
 * UUID v7 is time-ordered, providing better database indexing performance.
 * Used for:
 * - Payment receipt numbers (Payment.receiptNumber)
 * - Internal payment references (Payment.paymentReference for CARD/CASH)
 *
 * @author Oualid Gharach
 */
@Component
public class UuidGenerator {

    /**
     * Generate a unique UUID v7 string.
     * Format: xxxxxxxx-xxxx-7xxx-xxxx-xxxxxxxxxxxx (36 characters)
     *
     * @return UUID v7 string
     */
    public String generate() {
        UUID uuid = UuidCreator.getTimeOrderedEpoch();
        return uuid.toString();
    }
}