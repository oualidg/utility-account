/*
 * Copyright (c) 2026 Oualid Gharach. All rights reserved.
 *
 * Aiming for production-grade standards through clean code and best practices
 * for educational and informational purposes.
 *
 * Created on: 2/28/2026 at 2:02 PM
 *
 * Feel free to use or contribute. Contact: oualid.gharach@gmail.com
 */
package com.mycompany.api.account.dto;

/**
 * Response body for {@code POST /api/v1/users/{id}/reset-password}.
 *
 * <p>The temporary password is shown once — the admin must communicate
 * it to the user securely. The user should change it on next login.</p>
 *
 * @author Oualid Gharach
 */
public record ResetPasswordResponse(String temporaryPassword) {}
