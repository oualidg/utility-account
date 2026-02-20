/*
 * Copyright (c) 2026 Oualid Gharach. All rights reserved.
 *
 * Aiming for production-grade standards through clean code and best practices
 * for educational and informational purposes.
 *
 * Created on: 2/3/2026 at 6:26 PM
 *
 * Feel free to use or contribute. Contact: oualid.gharach@gmail.com
 */
package com.mycompany.api.account.util;

import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;

/**
 * Luhn-validated identifier generator and validator.
 * Supports generation of 8-digit Customer IDs and 10-digit Account Numbers.
 *
 * <p>Generation methods are instance methods (injectable, mockable for testing
 * collision retry). Validation methods remain static since they are pure functions
 * used in annotation validators and tests.</p>
 *
 * @author Oualid Gharach
 */
@Component
public class LuhnGenerator {

    // Pre-calculated powers of 10 for performance
    private static final long[] POWERS_OF_10 = {
            1L, 10L, 100L, 1000L, 10000L, 100000L, 1000000L, 10000000L, 100000000L, 1000000000L
    };

    /**
     * Generates an 8-digit Luhn-validated Customer ID.
     * Format: 7 random digits + 1 checksum digit
     *
     * @return 8-digit Customer ID as Long
     */
    public Long generateCustomerId() {
        return generateLuhnNumber(8);
    }

    /**
     * Generates a 10-digit Luhn-validated Account Number.
     * Format: 9 random digits + 1 checksum digit
     *
     * @return 10-digit Account Number as Long
     */
    public Long generateAccountNumber() {
        return generateLuhnNumber(10);
    }

    /**
     * Generic method to generate a Luhn-validated number of specified length.
     *
     * @param length total length including checksum digit (8 or 10)
     * @return Luhn-validated number
     * @throws IllegalArgumentException if length is not between 2 and 19
     */
    private Long generateLuhnNumber(int length) {
        if (length < 8 || length > 10) {
            throw new IllegalArgumentException("Length must be between 8 and 10");
        }

        int baseDigits = length - 1;

        // Generate random base number (e.g., for 8-digit: 7 random digits)
        long min = (long) Math.pow(10, baseDigits - 1);
        long max = (long) Math.pow(10, baseDigits) - 1;
        long base = ThreadLocalRandom.current().nextLong(min, max + 1);

        // Calculate Luhn checksum
        int sum = IntStream.range(0, baseDigits)
                .map(i -> {
                    int digit = (int) ((base / POWERS_OF_10[i]) % 10);

                    // Double every second digit from right (positions 0, 2, 4, ...)
                    if (i % 2 == 0) {
                        digit <<= 1; // Multiply by 2
                        if (digit > 9) {
                            digit -= 9; // Subtract 9 if result > 9
                        }
                    }
                    return digit;
                })
                .sum();

        int checkDigit = (10 - (sum % 10)) % 10;

        return (base * 10) + checkDigit;
    }

    /**
     * Validates an 8-digit Customer ID.
     *
     * @param customerId the Customer ID to validate
     * @return true if valid 8-digit Luhn number
     */
    public static boolean isValidCustomerId(String customerId) {
        return customerId != null
                && customerId.length() == 8
                && isValid(customerId);
    }

    /**
     * Validates a 10-digit Account Number.
     *
     * @param accountNumber the Account Number to validate
     * @return true if valid 10-digit Luhn number
     */
    public static boolean isValidAccountNumber(String accountNumber) {
        return accountNumber != null
                && accountNumber.length() == 10
                && isValid(accountNumber);
    }

    /**
     * Validates a number using the Luhn algorithm.
     * Works for any length (8, 10, or other).
     *
     * @param number the number to validate as a String
     * @return true if valid Luhn number, false otherwise
     */
    private static boolean isValid(String number) {
        if (number == null || number.isEmpty()) {
            return false;
        }

        int length = number.length();
        int sum = 0;

        for (int i = 0; i < length; i++) {
            char c = number.charAt(i);

            // Validate digit
            if (c < '0' || c > '9') {
                return false;
            }

            int digit = c - '0';

            // Double every second digit from the right
            if ((length - 1 - i) % 2 == 1) {
                digit *= 2;
                if (digit > 9) {
                    digit -= 9;
                }
            }

            sum += digit;
        }

        return sum % 10 == 0;
    }


}