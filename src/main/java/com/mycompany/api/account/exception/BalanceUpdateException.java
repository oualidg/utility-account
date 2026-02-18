/*
 * Copyright (c) 2026 Oualid Gharach. All rights reserved.
 *
 * Aiming for production-grade standards through clean code and best practices
 * for educational and informational purposes.
 *
 * Created on: 2/18/2026 at 7:00 PM
 *
 * Feel free to use or contribute. Contact: oualid.gharach@gmail.com
 */
package com.mycompany.api.account.exception;

/**
 * Thrown when an account balance update fails due to a database constraint
 * (e.g., numeric overflow, check constraint violation).
 *
 * @author Oualid Gharach
 */
public class BalanceUpdateException extends RuntimeException {

    public BalanceUpdateException(String message) {
        super(message);
    }
}