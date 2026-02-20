/*
 * Copyright (c) 2026 Oualid Gharach. All rights reserved.
 *
 * Aiming for production-grade standards through clean code and best practices
 * for educational and informational purposes.
 *
 * Created on: 2/7/2026 at 1:46 PM
 *
 * Feel free to use or contribute. Contact: oualid.gharach@gmail.com
 */
package com.mycompany.api.account.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for UuidGenerator utility.
 * Verifies UUID v7 generation and uniqueness.
 */
class UuidGeneratorTest {

    private UuidGenerator uuidGenerator;

    @BeforeEach
    void setUp() {
        uuidGenerator = new UuidGenerator();
    }

    @Test
    @DisplayName("Should generate a valid 36-character UUID v7 string")
    void shouldGenerateValidUuidV7String() {
        // Act
        String result = uuidGenerator.generate();

        // Assert
        assertThat(result)
                .isNotNull()
                .hasSize(36);

        UUID uuid = UUID.fromString(result);
        assertThat(uuid.version()).isEqualTo(7); // Must be version 7
    }

    @Test
    @DisplayName("Should generate unique IDs in a high-concurrency loop")
    void shouldGenerateUniqueIds() {
        // Arrange
        int count = 1000;
        Set<String> uuids = new HashSet<>();

        // Act
        for (int i = 0; i < count; i++) {
            uuids.add(uuidGenerator.generate());
        }

        // Assert
        // Set prevents duplicates; size should match count if all are unique
        assertThat(uuids).hasSize(count);
    }

    @Test
    @DisplayName("Should generate IDs with correct format and variant")
    void shouldGenerateCorrectFormat() {
        // Act
        String result = uuidGenerator.generate();
        UUID uuid = UUID.fromString(result);

        // Assert
        assertThat(result).matches("^[0-9a-f]{8}-[0-9a-f]{4}-7[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$");
        assertThat(uuid.variant()).isEqualTo(2); // Leach-Salz variant
    }
}