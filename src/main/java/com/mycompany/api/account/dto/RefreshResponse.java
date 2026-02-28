/*
 * Copyright (c) 2026 Oualid Gharach. All rights reserved.
 *
 * Aiming for production-grade standards through clean code and best practices
 * for educational and informational purposes.
 *
 * Created on: 2/27/2026 at 4:02 PM
 *
 * Feel free to use or contribute. Contact: oualid.gharach@gmail.com
 */
package com.mycompany.api.account.dto;

/**
 * Response body for {@code POST /api/auth/refresh} in bearer mode.
 * Returns the new access token for mobile apps and API tooling.
 */
public record RefreshResponse(String accessToken) {}