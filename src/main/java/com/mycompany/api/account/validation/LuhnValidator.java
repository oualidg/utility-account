/*
 * Copyright (c) 2026 Oualid Gharach. All rights reserved.
 *
 * Aiming for production-grade standards through clean code and best practices
 * for educational and informational purposes.
 *
 * Created on: 2/5/2026 at 6:54 PM
 *
 * Feel free to use or contribute. Contact: oualid.gharach@gmail.com
 */
package com.mycompany.api.account.validation;

import com.mycompany.api.account.util.LuhnGenerator;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Validator implementation for the @ValidLuhn annotation.
 * Validates that a Long value has a valid Luhn checksum and correct length.
 *
 * @author Oualid Gharach
 */
public class LuhnValidator implements ConstraintValidator<ValidLuhn, Long> {

    private int expectedLength;

    @Override
    public void initialize(ValidLuhn constraintAnnotation) {
        this.expectedLength = constraintAnnotation.length();
    }

    /**
     * Validate the provided Long value against Luhn algorithm and length.
     *
     * @param value the value to validate (can be null)
     * @param context validation context
     * @return true if valid or null, false otherwise
     */
    @Override
    public boolean isValid(Long value, ConstraintValidatorContext context) {
        // Null values are considered valid (use @NotNull separately if needed)
        if (value == null) {
            return true;
        }

        String valueStr = value.toString();

        // Check if length matches expected length
        if (valueStr.length() != expectedLength) {
            return false;
        }

        // Validate using appropriate LuhnGenerator method based on length
        if (expectedLength == 8) {
            return LuhnGenerator.isValidCustomerId(valueStr);
        } else if (expectedLength == 10) {
            return LuhnGenerator.isValidAccountNumber(valueStr);
        }

        // Unsupported length
        return false;
    }
}