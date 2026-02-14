/*
 * Copyright (c) 2026 Oualid Gharach. All rights reserved.
 *
 * Aiming for production-grade standards through clean code and best practices
 * for educational and informational purposes.
 *
 * Created on: 2/6/2026 at 5:39 PM
 *
 * Feel free to use or contribute. Contact: oualid.gharach@gmail.com
 */

package com.mycompany.api.account.model;

import lombok.Getter;

/**
 * Enumeration of payment providers.
 * Represents mobile money services for payment processing.
 * This will be moved to database configuration in future phases
 * to allow dynamic provider management and webhook authentication.
 *
 * @author Oualid Gharach
 */
@Getter
public enum PaymentProvider {

    /**
     * M-Pesa mobile money payment provider.
     */
    MPESA("M-Pesa"),

    /**
     * MTN Mobile Money payment provider.
     */
    MTN_MOMO("MTN Mobile Money"),

    /**
     * Airtel Money mobile payment provider.
     */
    AIRTEL_MONEY("Airtel Money");

    private final String displayName;

    PaymentProvider(String displayName) {
        this.displayName = displayName;
    }


}