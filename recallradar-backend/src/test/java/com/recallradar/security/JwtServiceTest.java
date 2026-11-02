package com.recallradar.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService("SecretKeyForJwtUnitTestTestingPurposeAntigravityProduction2026!", 3600000L, new ObjectMapper());
    }

    @Test
    void testGenerateAndValidateToken() {
        String token = jwtService.generateToken("admin", "ADMIN");
        assertNotNull(token);
        assertTrue(jwtService.validateToken(token));
        assertEquals("admin", jwtService.extractUsername(token));
        assertEquals("ADMIN", jwtService.extractRole(token));
    }

    @Test
    void testTamperedTokenFailsValidation() {
        String token = jwtService.generateToken("analyst", "ANALYST");
        String[] parts = token.split("\\.");
        String tampered = parts[0] + "." + parts[1] + ".invalidSignature123";

        assertFalse(jwtService.validateToken(tampered));
    }

    @Test
    void testExpiredTokenFailsValidation() {
        // Expiration in the past (-10 seconds)
        JwtService expiredJwtService = new JwtService("SecretKeyForJwtUnitTestTestingPurposeAntigravityProduction2026!", -10000L, new ObjectMapper());
        String token = expiredJwtService.generateToken("viewer", "VIEWER");

        assertFalse(expiredJwtService.validateToken(token));
    }
}
