/*
 * Copyright (c) 2026 Oualid Gharach. All rights reserved.
 *
 * Aiming for production-grade standards through clean code and best practices
 * for educational and informational purposes.
 *
 * Created on: 2/3/2026 at 8:21 PM
 *
 * Feel free to use or contribute. Contact: oualid.gharach@gmail.com
 */
package com.mycompany.api.account.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.Map;

/**
 * Standard error response for API errors.
 *
 * @author Oualid Gharach
 */
@JsonInclude(JsonInclude.Include.NON_NULL) // Don't include null fields in JSON
public record ErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        Map<String, String> validationErrors
) {
    /**
     * Create error response without validation errors
     */
    public static ErrorResponse of(HttpStatus httpStatus, String message, String path) {
        return new ErrorResponse(
                Instant.now(),
                httpStatus.value(),
                httpStatus.getReasonPhrase(),
                message,
                path,
                null
        );
    }

    /**
     * Create error response with validation errors
     */
    public static ErrorResponse ofValidation(String message, String path, Map<String, String> validationErrors) {
        return new ErrorResponse(
                Instant.now(),
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                message,
                path,
                validationErrors
        );
    }
}
