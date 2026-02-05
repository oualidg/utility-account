/*
 * Copyright (c) 2026 Oualid Gharach. All rights reserved.
 *
 * Aiming for production-grade standards through clean code and best practices
 * for educational and informational purposes.
 *
 * Created on: 2/3/2026 at 8:23 PM
 *
 * Feel free to use or contribute. Contact: oualid.gharach@gmail.com
 */
package com.mycompany.api.account.exception;

import com.mycompany.api.account.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler for the application.
 * Catches exceptions thrown by controllers and returns consistent error responses.
 *
 * @author Oualid Gharach
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles ResourceNotFoundException (404 Not Found)
     * Thrown when a requested resource doesn't exist.
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(
            ResourceNotFoundException ex,
            HttpServletRequest request) {

        log.error("Resource not found: {}", ex.getMessage());

        ErrorResponse errorResponse = ErrorResponse.of(
                HttpStatus.NOT_FOUND,
                ex.getMessage(),
                request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    /**
     * Handles DuplicateResourceException (409 Conflict)
     * Thrown when trying to create a resource that already exists.
     */
    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateResource(
            DuplicateResourceException ex,
            HttpServletRequest request) {

        log.error("Duplicate resource: {}", ex.getMessage());

        ErrorResponse errorResponse = ErrorResponse.of(
                HttpStatus.CONFLICT,
                ex.getMessage(),
                request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }

    /**
     * Handles validation errors (400 Bad Request)
     * Triggered when @Valid fails on request body validation.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationErrors(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {

        // Collect all field validation errors into a map
        Map<String, String> validationErrors = new HashMap<>();

        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            validationErrors.put(fieldName, errorMessage);
        });

        log.error("Validation failed: {}", validationErrors);

        ErrorResponse errorResponse = ErrorResponse.ofValidation(
                "Validation failed for one or more fields",
                request.getRequestURI(),
                validationErrors
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * Handle validation errors from @Valid annotations on path variables and parameters.
     *
     * @param ex the exception
     * @param request the web request
     * @return error response with validation details
     */
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ErrorResponse> handleMethodValidationException(
            HandlerMethodValidationException ex,
            HttpServletRequest request) {

        // Extract validation error messages
        Map<String, String> errors = new HashMap<>();
        ex.getValueResults().forEach(result -> {
            String parameterName = result.getMethodParameter().getParameterName();
            result.getResolvableErrors().forEach(error -> {
                errors.put(parameterName != null ? parameterName : "parameter", error.getDefaultMessage());
            });
        });

        log.error("Validation failed: {}", errors);

        String message = errors.isEmpty() ? "Validation failed" : errors.values().iterator().next();

        ErrorResponse errorResponse = ErrorResponse.ofValidation(
                message,
                request.getRequestURI(),
                errors
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * Handles IllegalArgumentException (400 Bad Request)
     * Thrown when invalid arguments are passed to methods.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(
            IllegalArgumentException ex,
            HttpServletRequest request) {

        log.error("Illegal argument: {}", ex.getMessage());

        ErrorResponse errorResponse = ErrorResponse.of(
                HttpStatus.BAD_REQUEST,
                ex.getMessage(),
                request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * Handles all other unexpected exceptions (500 Internal Server Error)
     * This is a catch-all for errors we didn't anticipate.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex,
            HttpServletRequest request) {

        log.error("Unexpected error occurred", ex);

        ErrorResponse errorResponse = ErrorResponse.of(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred. Please try again later.",
                request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
}