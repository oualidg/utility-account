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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

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

        log.warn("Resource not found: {}", ex.getMessage());

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

        log.warn("Duplicate resource: {}", ex.getMessage());

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

        log.warn("Validation failed for {} {} | Errors: {}",
                request.getMethod(),
                request.getRequestURI(),
                validationErrors);

        ErrorResponse errorResponse = ErrorResponse.ofValidation(
                "Validation failed for one or more fields",
                request.getRequestURI(),
                validationErrors
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * Handles validation errors on path variables and request parameters (400 Bad Request).
     * Triggered when @Valid annotation fails on @PathVariable or @RequestParam.
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
        ex.getParameterValidationResults().forEach(result -> {
            String parameterName = result.getMethodParameter().getParameterName();
            result.getResolvableErrors().forEach(error -> {
                errors.put(parameterName != null ? parameterName : "parameter", error.getDefaultMessage());
            });
        });

        log.warn("Validation failed for {} {} | Errors: {}",
                request.getMethod(),
                request.getRequestURI(),
                errors);

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
     * Thrown for invalid input arguments.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(
            IllegalArgumentException ex,
            HttpServletRequest request) {

        log.warn("Invalid argument: {}", ex.getMessage());

        ErrorResponse errorResponse = ErrorResponse.of(
                HttpStatus.BAD_REQUEST,
                ex.getMessage(),
                request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * Handles JSON parsing errors and invalid request body content (400 Bad Request).
     * Triggered when the request body cannot be deserialized into the expected DTO.
     *
     * @param ex the JSON parsing exception
     * @param request the HTTP request that triggered the exception
     * @return 400 response with specific error details
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex,
            HttpServletRequest request) {

        String message = "Invalid request payload: one or more fields contain invalid values.";

        if (ex.getCause() instanceof com.fasterxml.jackson.databind.exc.InvalidFormatException formatException) {
                message = String.format("Invalid value '%s' for field '%s'",
                        formatException.getValue(),
                        formatException.getPath().getFirst().getFieldName());
        }

        log.warn("Payload mapping failed: {}", message);

        ErrorResponse errorResponse = ErrorResponse.of(
                HttpStatus.BAD_REQUEST,
                message,
                request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * Handles type conversion errors for path variables and request parameters (400 Bad Request).
     * Triggered when Spring cannot convert a path variable or @RequestParam to the expected type.
     **
     * @param ex the type mismatch exception
     * @param request the HTTP request that triggered the exception
     * @return 400 response with user-friendly error message
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex,
            HttpServletRequest request) {

        String fieldName = ex.getName();
        String message = String.format("Invalid value provided for parameter: '%s'", fieldName);

        // Internal log to see what the user actually typed
        String targetType = (ex.getRequiredType() != null)
                ? ex.getRequiredType().getSimpleName()
                : "unknown type";
        log.warn("Parameter binding failed: {} = '{}'. Expected type: {}",
                fieldName, ex.getValue(), targetType);

        ErrorResponse errorResponse = ErrorResponse.of(
                HttpStatus.BAD_REQUEST,
                message,
                request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * Handles DataIntegrityViolationException (400 Bad Request).
     *
     * <p>This is a safety net for validation errors that slip past DTO validation.
     * Ideally, all validation should happen at the DTO level for fast failure.
     *
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(
            DataIntegrityViolationException ex,
            HttpServletRequest request) {

        String rootMessage = ex.getMostSpecificCause().getMessage();

        log.warn("Data integrity violation (validation should have caught this earlier): {}", rootMessage);

        // Extract user-friendly message
        String userMessage = "Invalid data provided. Please check your input and try again.";

        ErrorResponse errorResponse = ErrorResponse.of(
                HttpStatus.BAD_REQUEST,
                userMessage,
                request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * Handles all other unexpected exceptions (500 Internal Server Error)
     * This is a catch-all handler for any exceptions not handled by specific handlers.
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