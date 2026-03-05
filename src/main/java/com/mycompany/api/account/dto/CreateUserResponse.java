/*
 * Copyright (c) 2026 Oualid Gharach. All rights reserved.
 *
 * Aiming for production-grade standards through clean code and best practices
 * for educational and informational purposes.
 *
 * Created on: 3/3/2026 at 7:25 PM
 *
 * Feel free to use or contribute. Contact: oualid.gharach@gmail.com
 */
package com.mycompany.api.account.dto;

/**
 * Response for user creation — includes the user data and the
 * generated temporary password shown once to the admin.
 * @author Oualid Gharach
 */
public record CreateUserResponse(UserResponse user, String temporaryPassword) {}