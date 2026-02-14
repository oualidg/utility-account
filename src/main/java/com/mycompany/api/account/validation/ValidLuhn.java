/*
 * Copyright (c) 2026 Oualid Gharach. All rights reserved.
 *
 * Aiming for production-grade standards through clean code and best practices
 * for educational and informational purposes.
 *
 * Created on: 2/5/2026 at 6:57 PM
 *
 * Feel free to use or contribute. Contact: oualid.gharach@gmail.com
 */
package com.mycompany.api.account.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * Custom validation annotation to validate Luhn checksum for customer IDs and account numbers.
 * Usage examples:
 * - @ValidLuhn(length = 8) for customer IDs (8-digit)
 * - @ValidLuhn(length = 10) for account numbers (10-digit)
 *
 * @author Oualid Gharach
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = LuhnValidator.class)
@Documented
public @interface ValidLuhn {

    String message() default "Invalid Luhn checksum";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    int length();
}