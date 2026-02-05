/*
 * Copyright (c) 2026 Oualid Gharach. All rights reserved.
 *
 * Aiming for production-grade standards through clean code and best practices
 * for educational and informational purposes.
 *
 * Created on: 2/3/2026 at 8:20 PM
 *
 * Feel free to use or contribute. Contact: oualid.gharach@gmail.com
 */
package com.mycompany.api.account.exception;

/**
 * Exception thrown when attempting to create a resource that already exists.
 * This is a runtime exception that will be caught by the global exception handler.
 *
 * @author Oualid Gharach
 */
public class DuplicateResourceException extends RuntimeException {

    /**
     * Constructs a new DuplicateResourceException with the specified detail message.
     *
     * @param message the detail message
     */
    public DuplicateResourceException(String message) {
        super(message);
    }

    /**
     * Constructs a new DuplicateResourceException with the specified detail message and cause.
     *
     * @param message the detail message
     * @param cause the cause of the exception
     */
    public DuplicateResourceException(String message, Throwable cause) {
        super(message, cause);
    }
}