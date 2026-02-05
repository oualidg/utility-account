/*
 * Copyright (c) 2026 Oualid Gharach. All rights reserved.
 *
 * Aiming for production-grade standards through clean code and best practices
 * for educational and informational purposes.
 *
 * Created on: 2/3/2026 at 6:36 PM
 *
 * Feel free to use or contribute. Contact: oualid.gharach@gmail.com
 */
package com.mycompany.api.account.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for LuhnGenerator utility class.
 * Tests ID generation and validation functionality.
 *
 * @author Oualid Gharach
 */
@DisplayName("LuhnGenerator Unit Tests")
class LuhnGeneratorTest {

    @Test
    void shouldGenerateValid8DigitCustomerId() {
        // Act
        Long customerId = LuhnGenerator.generateCustomerId();

        // Assert
        assertNotNull(customerId, "Customer ID should not be null");
        assertEquals(8, customerId.toString().length(), "Customer ID should be exactly 8 digits");
        assertTrue(LuhnGenerator.isValidCustomerId(customerId.toString()),
                "Generated Customer ID should pass Luhn validation");
    }

    @Test
    void shouldGenerateValid10DigitAccountNumber() {
        // Act
        Long accountNumber = LuhnGenerator.generateAccountNumber();

        // Assert
        assertNotNull(accountNumber, "Account Number should not be null");
        assertEquals(10, accountNumber.toString().length(), "Account Number should be exactly 10 digits");
        assertTrue(LuhnGenerator.isValidAccountNumber(accountNumber.toString()),
                "Generated Account Number should pass Luhn validation");
    }

    @Test
    void shouldRejectInvalidLuhnChecksums() {
        // Invalid checksums for Customer IDs
        assertFalse(LuhnGenerator.isValidCustomerId("12345671"),
                "Invalid checksum should fail validation");
        assertFalse(LuhnGenerator.isValidCustomerId("12345679"),
                "Invalid checksum should fail validation");

        // Invalid checksums for Account Numbers
        assertFalse(LuhnGenerator.isValidAccountNumber("1234567890"),
                "Invalid checksum should fail validation");
        assertFalse(LuhnGenerator.isValidAccountNumber("1234567891"),
                "Invalid checksum should fail validation");
    }

    @Test
    void shouldRejectInvalidLengthForCustomerId() {
        // Too short
        assertFalse(LuhnGenerator.isValidCustomerId("1234567"),
                "7-digit number should be rejected");

        // Too long
        assertFalse(LuhnGenerator.isValidCustomerId("123456789"),
                "9-digit number should be rejected");

        // Way too short
        assertFalse(LuhnGenerator.isValidCustomerId("123"),
                "Short number should be rejected");
    }

    @Test
    void shouldRejectInvalidLengthForAccountNumber() {
        // Too short
        assertFalse(LuhnGenerator.isValidAccountNumber("123456789"),
                "9-digit number should be rejected");

        // Too long
        assertFalse(LuhnGenerator.isValidAccountNumber("12345678901"),
                "11-digit number should be rejected");

        // Wrong length (8 digits)
        assertFalse(LuhnGenerator.isValidAccountNumber("12345678"),
                "8-digit number should be rejected for Account Number");
    }

    @Test
    void shouldRejectNullValues() {
        assertFalse(LuhnGenerator.isValidCustomerId(null),
                "Null Customer ID should be rejected");
        assertFalse(LuhnGenerator.isValidAccountNumber(null),
                "Null Account Number should be rejected");
    }

    @Test
    void shouldRejectEmptyStrings() {
        assertFalse(LuhnGenerator.isValidCustomerId(""),
                "Empty Customer ID should be rejected");
        assertFalse(LuhnGenerator.isValidAccountNumber(""),
                "Empty Account Number should be rejected");
    }

    @Test
    void shouldRejectNonNumericCharacters() {
        assertFalse(LuhnGenerator.isValidCustomerId("1234567A"),
                "Customer ID with letters should be rejected");
        assertFalse(LuhnGenerator.isValidCustomerId("12-34-56-70"),
                "Customer ID with dashes should be rejected");
        assertFalse(LuhnGenerator.isValidAccountNumber("123456789X"),
                "Account Number with letters should be rejected");
    }

    @Test
    void shouldGenerate1000UniqueCustomerIds() {
        // This tests that we can generate many IDs without collisions
        Set<Long> ids = new HashSet<>();

        for (int i = 0; i < 1000; i++) {
            Long id = LuhnGenerator.generateCustomerId();
            assertTrue(ids.add(id), "Generated ID should be unique: " + id);
        }
   }

    @Test
    void shouldGenerate1000UniqueAccountNumbers() {
        // This tests that we can generate many Account Numbers without collisions
        Set<Long> numbers = new HashSet<>();

        for (int i = 0; i < 1000; i++) {
            Long number = LuhnGenerator.generateAccountNumber();
            assertTrue(numbers.add(number), "Generated Account Number should be unique: " + number);
        }
    }
}