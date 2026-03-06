/*
 * Copyright (c) 2026 Oualid Gharach. All rights reserved.
 *
 * Aiming for production-grade standards through clean code and best practices
 * for educational and informational purposes.
 *
 * Created on: 3/6/2026 at 9:34 PM
 *
 * Feel free to use or contribute. Contact: oualid.gharach@gmail.com
 */
package com.mycompany.api.account.util;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Utility class for generating cryptographically secure temporary passwords.
 *
 * <p>Temporary passwords are generated for new users and password resets.
 * They are intended to be changed by the user on first login.</p>
 *
 * <p>Each generated password is guaranteed to contain at least one character
 * from each of the following categories: uppercase letter, lowercase letter,
 * digit, and special character.</p>
 *
 * <p>Character set deliberately excludes visually ambiguous characters
 * ({@code 0}, {@code O}, {@code l}, {@code 1}, {@code I}) to reduce
 * transcription errors when passwords are communicated verbally or printed.</p>
 *
 * @author Oualid Gharach
 */
public final class PasswordGenerator {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private static final String UPPERCASE = "ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final String LOWERCASE = "abcdefghjkmnpqrstuvwxyz";
    private static final String DIGITS    = "23456789";
    private static final String SPECIAL   = "!@#$%";
    private static final String ALL       = UPPERCASE + LOWERCASE + DIGITS + SPECIAL;

    private static final int DEFAULT_LENGTH = 12;

    private PasswordGenerator() {}

    /**
     * Generate a cryptographically secure temporary password of default length (12).
     * Guaranteed to contain at least one uppercase letter, one lowercase letter,
     * one digit, and one special character.
     *
     * @return temporary password string
     */
    public static String generate() {
        return generate(DEFAULT_LENGTH);
    }

    /**
     * Generate a cryptographically secure temporary password of specified length.
     * Guaranteed to contain at least one character from each required category.
     * Minimum length is 4 (one per category).
     *
     * @param length desired password length (minimum 4)
     * @return temporary password string
     * @throws IllegalArgumentException if length is less than 4
     */
    public static String generate(int length) {
        if (length < 4) {
            throw new IllegalArgumentException(
                    "Password length must be at least 4 to satisfy all character requirements");
        }

        List<Character> chars = new ArrayList<>(length);

        // Guarantee one from each required category
        chars.add(randomChar(UPPERCASE));
        chars.add(randomChar(LOWERCASE));
        chars.add(randomChar(DIGITS));
        chars.add(randomChar(SPECIAL));

        // Fill remaining positions from full character set
        for (int i = 4; i < length; i++) {
            chars.add(randomChar(ALL));
        }

        // Shuffle to avoid predictable category positions
        // (e.g. special char always last would be a pattern)
        Collections.shuffle(chars, SECURE_RANDOM);

        StringBuilder password = new StringBuilder(length);
        for (char c : chars) {
            password.append(c);
        }
        return password.toString();
    }

    private static char randomChar(String source) {
        return source.charAt(SECURE_RANDOM.nextInt(source.length()));
    }
}