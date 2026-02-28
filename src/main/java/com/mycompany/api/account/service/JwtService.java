/*
 * Copyright (c) 2026 Oualid Gharach. All rights reserved.
 *
 * Aiming for production-grade standards through clean code and best practices
 * for educational and informational purposes.
 *
 * Created on: 2/24/2026 at 9:18 PM
 *
 * Feel free to use or contribute. Contact: oualid.gharach@gmail.com
 */
package com.mycompany.api.account.service;

import com.mycompany.api.account.entity.User;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.*;
import com.nimbusds.jwt.*;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.text.ParseException;
import java.time.Instant;
import java.util.Date;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

/**
 * Service responsible for issuing and validating JWT access and refresh tokens.
 *
 * <p>Uses Nimbus JOSE+JWT with HMAC-SHA256 (HS256) symmetric signing.
 * The secret key is externalized to application properties and injected
 * at startup — never hardcoded.</p>
 *
 * <p>Access tokens are short-lived (default 15 minutes) and carry the
 * username and role as claims. Refresh tokens are long-lived (default 24 hours)
 * and carry only the username — they are used solely to issue new access tokens.</p>
 *
 * @author Oualid Gharach
 */
@Service
@Slf4j
public class JwtService {

    private static final String CLAIM_ROLE = "role";
    private static final String CLAIM_TOKEN_TYPE = "type";
    private static final String TOKEN_TYPE_ACCESS = "access";
    private static final String TOKEN_TYPE_REFRESH = "refresh";

    private final SecretKey signingKey;
    @Getter
    private final long accessTokenExpiry;
    @Getter
    private final long refreshTokenExpiry;

    public JwtService(
            @Value("${app.jwt.secret}") String secretHex,
            @Value("${app.jwt.access-token-expiry}") long accessTokenExpiry,
            @Value("${app.jwt.refresh-token-expiry}") long refreshTokenExpiry) {
        byte[] keyBytes = HexFormat.of().parseHex(secretHex);
        this.signingKey = new SecretKeySpec(keyBytes, "HmacSHA256");
        this.accessTokenExpiry = accessTokenExpiry;
        this.refreshTokenExpiry = refreshTokenExpiry;
    }

    /**
     * Issue a short-lived access token for the given user.
     *
     * @param user the authenticated user
     * @return signed JWT access token string
     */
    public String issueAccessToken(User user) {
        return buildToken(user.getUsername(), user.getRole().name(), TOKEN_TYPE_ACCESS, accessTokenExpiry);
    }

    /**
     * Issue a long-lived refresh token for the given user.
     *
     * @param user the authenticated user
     * @return signed JWT refresh token string
     */
    public String issueRefreshToken(User user) {
        return buildToken(user.getUsername(), null, TOKEN_TYPE_REFRESH, refreshTokenExpiry);
    }

    /**
     * Validate a token and extract its claims.
     *
     * <p>Returns empty if the token is expired, tampered with, or malformed.</p>
     *
     * @param token the raw JWT string
     * @return the verified claims, or empty if invalid
     */
    public Optional<JWTClaimsSet> validate(String token) {
        try {
            SignedJWT signed = SignedJWT.parse(token);
            JWSVerifier verifier = new MACVerifier(signingKey);

            if (!signed.verify(verifier)) {
                log.warn("JWT signature verification failed");
                return Optional.empty();
            }

            JWTClaimsSet claims = signed.getJWTClaimsSet();

            if (claims.getExpirationTime().before(Date.from(Instant.now()))) {
                log.debug("JWT token expired for subject: {}", claims.getSubject());
                return Optional.empty();
            }

            return Optional.of(claims);

        } catch (ParseException | JOSEException e) {
            log.warn("JWT validation failed: {}", e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Extract the token type claim ("access" or "refresh").
     *
     * @param claims verified claims from {@link #validate(String)}
     * @return token type string
     */
    public String getTokenType(JWTClaimsSet claims) {
        return (String) claims.getClaim(CLAIM_TOKEN_TYPE);
    }

    /**
     * Extract the username (subject) from verified claims.
     *
     * @param claims verified claims from {@link #validate(String)}
     * @return username
     */
    public String getUsername(JWTClaimsSet claims) {
        return claims.getSubject();
    }

    /**
     * Extract the role claim from verified claims.
     *
     * @param claims verified claims from {@link #validate(String)}
     * @return role string e.g. "ROLE_ADMIN"
     */
    public String getRole(JWTClaimsSet claims) {
        return (String) claims.getClaim(CLAIM_ROLE);
    }


    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private String buildToken(String username, String role, String tokenType, long expirySeconds) {
        try {
            Instant now = Instant.now();

            JWTClaimsSet.Builder claimsBuilder = new JWTClaimsSet.Builder()
                    .subject(username)
                    .issueTime(Date.from(now))
                    .expirationTime(Date.from(now.plusSeconds(expirySeconds)))
                    .jwtID(UUID.randomUUID().toString())
                    .claim(CLAIM_TOKEN_TYPE, tokenType);

            if (role != null) {
                claimsBuilder.claim(CLAIM_ROLE, role);
            }

            SignedJWT jwt = new SignedJWT(
                    new JWSHeader(JWSAlgorithm.HS256),
                    claimsBuilder.build()
            );

            jwt.sign(new MACSigner(signingKey));
            return jwt.serialize();

        } catch (JOSEException e) {
            throw new IllegalStateException("Failed to sign JWT", e);
        }
    }
}