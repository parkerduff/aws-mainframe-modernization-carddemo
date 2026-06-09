package com.aws.carddemo.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Generates and validates JWT tokens. The token payload carries the equivalent of the
 * COBOL COMMAREA general info (COCOM01Y.cpy): the user id (subject), the user type, and
 * the "from program" context.
 */
@Component
public class JwtTokenProvider {

    private final SecretKey signingKey;
    private final long expirationMs;

    public JwtTokenProvider(
            @Value("${carddemo.jwt.secret}") String secret,
            @Value("${carddemo.jwt.expiration-ms}") long expirationMs) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    /**
     * Build a signed JWT for an authenticated user.
     *
     * @param userId      the SEC-USR-ID / CDEMO-USER-ID
     * @param userType    'A' (admin) or 'U' (user)
     * @param fromProgram the COBOL program context (e.g. COSGN00C), preserved as a claim
     * @return a compact, signed JWT string
     */
    public String generateToken(String userId, String userType, String fromProgram) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);
        return Jwts.builder()
                .subject(userId)
                .claim("userType", userType)
                .claim("fromProgram", fromProgram)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(signingKey)
                .compact();
    }

    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException ex) {
            return false;
        }
    }

    public String getUserId(String token) {
        return parseClaims(token).getSubject();
    }

    public String getUserType(String token) {
        return parseClaims(token).get("userType", String.class);
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
