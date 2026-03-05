/*
 * Copyright (c) 2026 Oualid Gharach. All rights reserved.
 *
 * Aiming for production-grade standards through clean code and best practices
 * for educational and informational purposes.
 *
 * Created on: 2/24/2026 at 10:14 PM
 *
 * Feel free to use or contribute. Contact: oualid.gharach@gmail.com
 */
package com.mycompany.api.account.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Response body for {@code POST /api/auth/login}.
 *
 * <p>In cookie mode ({@code X-Auth-Mode: cookie} or default), tokens are issued
 * as HttpOnly cookies and {@code accessToken}/{@code refreshToken} are null —
 * excluded from the JSON response via {@code @JsonInclude(NON_NULL)}.</p>
 *
 * <p>In bearer mode ({@code X-Auth-Mode: bearer}), tokens are returned in the
 * response body and no cookies are set — intended for mobile apps and API tooling.</p>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record LoginResponse(
        Long id,
        String username,
        String firstName,
        String lastName,
        String role,
        String accessToken,
        String refreshToken
) {
    /**
     * Creates a LoginResponse without tokens — for /me endpoint and cookie mode responses.
     */
    public static LoginResponse withoutTokens(Long id, String username, String firstName, String lastName, String role) {
        return new LoginResponse(id, username, firstName, lastName, role, null, null);
    }

    /**
     * Returns a copy of this response without tokens — for cookie mode
     * where tokens are delivered as HttpOnly cookies, not in the body.
     */
    public LoginResponse toCookieResponse() {
        return new LoginResponse(id, username, firstName, lastName, role, null, null);
    }
}