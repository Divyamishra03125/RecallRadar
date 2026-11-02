package com.recallradar.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Enterprise Security Authorization Test Suite
 * Verifies BCrypt password hashing, JWT RBAC claim propagation,
 * and security context authorization across ADMIN, ANALYST, and VIEWER roles.
 */
class SecurityAuthorizationIntegrationTest {

    private PasswordEncoder passwordEncoder;
    private JwtService jwtService;
    private JwtAuthenticationFilter jwtFilter;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        passwordEncoder = new BCryptPasswordEncoder(10);
        jwtService = new JwtService("RecallRadarEnterpriseSecretKeyForProductionTokenSigning2026Antigravity!", 3600000L, null);
        jwtFilter = new JwtAuthenticationFilter(jwtService);
    }

    @Test
    @DisplayName("Verify BCrypt password hashing, salting, and resistance to mismatch")
    void testBCryptPasswordHashingAndVerification() {
        String rawPassword = "Admin@123!";
        String encodedHash = passwordEncoder.encode(rawPassword);

        assertNotNull(encodedHash);
        assertTrue(encodedHash.startsWith("$2a$10$") || encodedHash.startsWith("$2b$10$"),
                "BCrypt hash should follow standard $2a$ or $2b$ prefix with 10 salt rounds");

        // Verify valid match
        assertTrue(passwordEncoder.matches(rawPassword, encodedHash),
                "BCrypt must successfully verify the original plain password");

        // Verify rejection of incorrect passwords
        assertFalse(passwordEncoder.matches("WrongPassword@123", encodedHash),
                "BCrypt must reject invalid passwords");
        assertFalse(passwordEncoder.matches("admin@123!", encodedHash),
                "BCrypt must be case-sensitive");

        // Verify distinct salts produce different hashes for identical passwords
        String secondHash = passwordEncoder.encode(rawPassword);
        assertNotEquals(encodedHash, secondHash, "Subsequent BCrypt encodes must produce unique salt hashes");
        assertTrue(passwordEncoder.matches(rawPassword, secondHash));
    }

    @Test
    @DisplayName("Verify JWT Generation and Claims for ADMIN role")
    void testJwtAdminRoleClaims() {
        String token = jwtService.generateToken("admin", "ADMIN");
        assertNotNull(token);
        assertTrue(jwtService.validateToken(token));
        assertEquals("admin", jwtService.extractUsername(token));
        assertEquals("ADMIN", jwtService.extractRole(token));
    }

    @Test
    @DisplayName("Verify JWT Generation and Claims for ANALYST role")
    void testJwtAnalystRoleClaims() {
        String token = jwtService.generateToken("analyst", "ANALYST");
        assertNotNull(token);
        assertTrue(jwtService.validateToken(token));
        assertEquals("analyst", jwtService.extractUsername(token));
        assertEquals("ANALYST", jwtService.extractRole(token));
    }

    @Test
    @DisplayName("Verify JWT Generation and Claims for VIEWER role")
    void testJwtViewerRoleClaims() {
        String token = jwtService.generateToken("viewer", "VIEWER");
        assertNotNull(token);
        assertTrue(jwtService.validateToken(token));
        assertEquals("viewer", jwtService.extractUsername(token));
        assertEquals("VIEWER", jwtService.extractRole(token));
    }

    @Test
    @DisplayName("Verify JwtAuthenticationFilter grants ROLE_ADMIN authority in SecurityContext")
    void testJwtAuthenticationFilterInjectsAdminRole() throws Exception {
        String adminToken = jwtService.generateToken("superadmin", "ADMIN");

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + adminToken);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        jwtFilter.doFilter(request, response, filterChain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth, "Authentication must be established in SecurityContextHolder");
        assertEquals("superadmin", auth.getName());

        Collection<? extends GrantedAuthority> authorities = auth.getAuthorities();
        assertTrue(authorities.stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")),
                "Security context must contain ROLE_ADMIN authority");
    }

    @Test
    @DisplayName("Verify JwtAuthenticationFilter grants ROLE_ANALYST authority in SecurityContext")
    void testJwtAuthenticationFilterInjectsAnalystRole() throws Exception {
        String analystToken = jwtService.generateToken("analyst_jane", "ANALYST");

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + analystToken);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        jwtFilter.doFilter(request, response, filterChain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth);
        assertEquals("analyst_jane", auth.getName());

        Collection<? extends GrantedAuthority> authorities = auth.getAuthorities();
        assertTrue(authorities.stream().anyMatch(a -> a.getAuthority().equals("ROLE_ANALYST")),
                "Security context must contain ROLE_ANALYST authority");
        assertFalse(authorities.stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")),
                "Analyst must NOT have ROLE_ADMIN authority");
    }

    @Test
    @DisplayName("Verify JwtAuthenticationFilter grants ROLE_VIEWER authority in SecurityContext")
    void testJwtAuthenticationFilterInjectsViewerRole() throws Exception {
        String viewerToken = jwtService.generateToken("viewer_john", "VIEWER");

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + viewerToken);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        jwtFilter.doFilter(request, response, filterChain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth);
        assertEquals("viewer_john", auth.getName());

        Collection<? extends GrantedAuthority> authorities = auth.getAuthorities();
        assertTrue(authorities.stream().anyMatch(a -> a.getAuthority().equals("ROLE_VIEWER")),
                "Security context must contain ROLE_VIEWER authority");
        assertFalse(authorities.stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")),
                "Viewer must NOT have ROLE_ADMIN authority");
        assertFalse(authorities.stream().anyMatch(a -> a.getAuthority().equals("ROLE_ANALYST")),
                "Viewer must NOT have ROLE_ANALYST authority");
    }

    @Test
    @DisplayName("Verify Unauthenticated / Missing Bearer token leaves SecurityContext empty")
    void testUnauthenticatedRequestLeavesSecurityContextEmpty() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        jwtFilter.doFilter(request, response, filterChain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNull(auth, "Unauthenticated request without Authorization header must have null SecurityContext authentication");
    }

    @Test
    @DisplayName("Verify Invalid Bearer token is rejected and leaves SecurityContext empty")
    void testInvalidTokenLeavesSecurityContextEmpty() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer malformed.bogus.jwt.token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        jwtFilter.doFilter(request, response, filterChain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNull(auth, "Tampered or invalid token must be rejected without authenticating");
    }
}
