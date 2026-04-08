package com.cardemo.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenProviderTest {

    private JwtTokenProvider tokenProvider;

    @BeforeEach
    void setUp() {
        // Plain-text key for testing (must be at least 64 bytes for HS512)
        String secret = "carddemo-local-dev-key-not-for-production-use-must-be-at-least-64-bytes-long-for-hmac512";
        tokenProvider = new JwtTokenProvider(secret, 86400000L);
    }

    @Test
    void generateToken_producesValidToken() {
        String token = tokenProvider.generateToken("USER0001", "A");

        assertNotNull(token);
        assertTrue(token.length() > 0);
    }

    @Test
    void getUserIdFromToken_returnsCorrectUserId() {
        String token = tokenProvider.generateToken("USER0001", "A");

        String userId = tokenProvider.getUserIdFromToken(token);

        assertEquals("USER0001", userId);
    }

    @Test
    void getUserTypeFromToken_returnsCorrectUserType() {
        String token = tokenProvider.generateToken("USER0001", "A");

        String userType = tokenProvider.getUserTypeFromToken(token);

        assertEquals("A", userType);
    }

    @Test
    void validateToken_withValidToken_returnsTrue() {
        String token = tokenProvider.generateToken("USER0001", "A");

        assertTrue(tokenProvider.validateToken(token));
    }

    @Test
    void validateToken_withInvalidToken_returnsFalse() {
        assertFalse(tokenProvider.validateToken("invalid_token"));
    }
}
