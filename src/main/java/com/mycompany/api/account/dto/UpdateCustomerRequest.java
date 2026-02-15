/*
 * Copyright (c) 2026 Oualid Gharach. All rights reserved.
 *
 * Aiming for production-grade standards through clean code and best practices
 * for educational and informational purposes.
 *
 * Created on: 2/3/2026 at 9:50 PM
 *
 * Feel free to use or contribute. Contact: oualid.gharach@gmail.com
 */
package com.mycompany.api.account.dto;

import jakarta.validation.constraints.*;

/**
 * Request to update an existing customer.
 * All fields are optional - only provide fields you want to update (partial update).
 *
 * @author Oualid Gharach
 */
public record UpdateCustomerRequest(

        @Size(min = 2, max = 50, message = "First name must be between 2 and 50 characters")
        String firstName,

        @Size(min = 2, max = 50, message = "Last name must be between 2 and 50 characters")
        String lastName,

        @Email(message = "Email must be valid")
        @Size(max = 100, message = "Email must not exceed 100 characters")
        String email,

        @Size(max = 15, message = "Mobile number must not exceed 15 characters")
        @Pattern(regexp = "^\\+?[0-9]{7,14}$", message = "Mobile number must be 7-14 digits, optionally with + prefix")
        String mobileNumber
) {
}
