package com.recallradar.security;

import com.recallradar.security.dto.*;
import com.recallradar.user.AppUser;
import com.recallradar.user.AppUserRepository;
import com.recallradar.user.UserAuditLog;
import com.recallradar.user.UserAuditLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AppUserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private UserAuditLogRepository auditLogRepository;

    @InjectMocks
    private AuthController authController;

    private AppUser demoUser;

    @BeforeEach
    void setUp() {
        demoUser = new AppUser("testuser", "test@recallradar.com", "$2a$10$hashedpassword", "ANALYST", "Test User");
        demoUser.setAccountStatus("ACTIVE");
    }

    @Test
    @DisplayName("Verify successful login by username")
    void testLoginSuccessByUsername() {
        when(userRepository.findByUsernameIgnoreCaseOrEmailIgnoreCase("testuser", "testuser"))
                .thenReturn(Optional.of(demoUser));
        when(passwordEncoder.matches("Password@123", demoUser.getPasswordHash())).thenReturn(true);
        when(jwtService.generateToken("testuser", "ANALYST")).thenReturn("mocked.jwt.token");

        ResponseEntity<AuthResponse> response = authController.login(
                new LoginRequest("testuser", null, "Password@123")
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("mocked.jwt.token", response.getBody().token());
        assertEquals("testuser", response.getBody().username());
        assertEquals("ANALYST", response.getBody().role());
        verify(userRepository, times(1)).save(demoUser);
    }

    @Test
    @DisplayName("Verify successful login by email")
    void testLoginSuccessByEmail() {
        when(userRepository.findByUsernameIgnoreCaseOrEmailIgnoreCase("test@recallradar.com", "test@recallradar.com"))
                .thenReturn(Optional.of(demoUser));
        when(passwordEncoder.matches("Password@123", demoUser.getPasswordHash())).thenReturn(true);
        when(jwtService.generateToken("testuser", "ANALYST")).thenReturn("mocked.jwt.token");

        ResponseEntity<AuthResponse> response = authController.login(
                new LoginRequest(null, "test@recallradar.com", "Password@123")
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("testuser", response.getBody().username());
    }

    @Test
    @DisplayName("Verify login succeeds when requestedPortal matches user role")
    void testLoginWithMatchingPortalSuccess() {
        when(userRepository.findByUsernameIgnoreCaseOrEmailIgnoreCase("testuser", "testuser"))
                .thenReturn(Optional.of(demoUser));
        when(passwordEncoder.matches("Password@123", demoUser.getPasswordHash())).thenReturn(true);
        when(jwtService.generateToken("testuser", "ANALYST")).thenReturn("mocked.jwt.token");

        ResponseEntity<AuthResponse> response = authController.login(
                new LoginRequest("testuser", null, "Password@123", "ANALYST")
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("ANALYST", response.getBody().role());
    }

    @Test
    @DisplayName("Verify login fails with 403 when requestedPortal mismatches user role (Analyst attempting Admin portal)")
    void testLoginWithMismatchedPortalFails() {
        when(userRepository.findByUsernameIgnoreCaseOrEmailIgnoreCase("testuser", "testuser"))
                .thenReturn(Optional.of(demoUser));
        when(passwordEncoder.matches("Password@123", demoUser.getPasswordHash())).thenReturn(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                authController.login(new LoginRequest("testuser", null, "Password@123", "ADMIN"))
        );

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        assertTrue(ex.getReason().contains("not authorized to access the Admin Portal"));
    }

    @Test
    @DisplayName("Verify Viewer attempting Analyst portal is rejected with 403")
    void testViewerAttemptingAnalystPortalFails() {
        AppUser viewer = new AppUser("viewer", "viewer@test.com", "$2a$10$hash", "VIEWER", "Viewer User");
        viewer.setAccountStatus("ACTIVE");
        when(userRepository.findByUsernameIgnoreCaseOrEmailIgnoreCase("viewer", "viewer"))
                .thenReturn(Optional.of(viewer));
        when(passwordEncoder.matches("Password@123", viewer.getPasswordHash())).thenReturn(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                authController.login(new LoginRequest("viewer", null, "Password@123", "ANALYST"))
        );

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        assertTrue(ex.getReason().contains("not authorized to access the Analyst Portal"));
    }

    @Test
    @DisplayName("Verify pending approval Analyst account is rejected with 403 Forbidden")
    void testLoginPendingApprovalUserFails() {
        demoUser.setAccountStatus("PENDING_APPROVAL");
        demoUser.setEnabled(false);
        when(userRepository.findByUsernameIgnoreCaseOrEmailIgnoreCase("testuser", "testuser"))
                .thenReturn(Optional.of(demoUser));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                authController.login(new LoginRequest("testuser", null, "Password@123"))
        );

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        assertTrue(ex.getReason().contains("pending administrator approval"));
    }

    @Test
    @DisplayName("Verify disabled user cannot log in and receives 403 Forbidden")
    void testLoginDisabledUserFails() {
        demoUser.setEnabled(false);
        demoUser.setAccountStatus("DISABLED");
        when(userRepository.findByUsernameIgnoreCaseOrEmailIgnoreCase("testuser", "testuser"))
                .thenReturn(Optional.of(demoUser));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                authController.login(new LoginRequest("testuser", null, "Password@123"))
        );

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        assertTrue(ex.getReason().contains("account is disabled"));
    }

    @Test
    @DisplayName("Verify incorrect password returns 401 Unauthorized")
    void testLoginInvalidPasswordFails() {
        when(userRepository.findByUsernameIgnoreCaseOrEmailIgnoreCase("testuser", "testuser"))
                .thenReturn(Optional.of(demoUser));
        when(passwordEncoder.matches("WrongPassword", demoUser.getPasswordHash())).thenReturn(false);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                authController.login(new LoginRequest("testuser", null, "WrongPassword"))
        );

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Invalid username or password"));
    }

    @Test
    @DisplayName("Verify unknown user returns 401 Unauthorized")
    void testLoginUserNotFoundFails() {
        when(userRepository.findByUsernameIgnoreCaseOrEmailIgnoreCase("unknown", "unknown"))
                .thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                authController.login(new LoginRequest("unknown", null, "Password@123"))
        );

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
    }

    @Test
    @DisplayName("Verify Analyst registration creates account with PENDING_APPROVAL status and no token")
    void testAnalystRegistrationSuccess() {
        AnalystRegistrationRequest req = new AnalystRegistrationRequest(
                "Dr. John Watson",
                "jwatson",
                "watson@hospital.org",
                "Medical Review Board",
                "Safety Investigation",
                "Chief Investigator",
                "SecurePassword123!",
                "SecurePassword123!",
                true,
                "ADMIN" // Attacker attempts to pass role ADMIN in request
        );

        when(userRepository.existsByUsernameIgnoreCase("jwatson")).thenReturn(false);
        when(userRepository.existsByEmailIgnoreCase("watson@hospital.org")).thenReturn(false);
        when(passwordEncoder.encode("SecurePassword123!")).thenReturn("$2a$10$hashed123");
        when(userRepository.save(any(AppUser.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ResponseEntity<AnalystRegistrationResponse> response = authController.registerAnalyst(req);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("PENDING_APPROVAL", response.getBody().status());
        assertTrue(response.getBody().message().contains("awaiting administrator approval"));
        verify(userRepository, times(1)).save(argThat(u ->
                "ANALYST".equals(u.getRole()) &&
                "PENDING_APPROVAL".equals(u.getAccountStatus()) &&
                !u.isEnabled()
        ));
    }

    @Test
    @DisplayName("Verify public registration strictly forces VIEWER role")
    void testRegisterForcesViewerRole() {
        when(userRepository.existsByUsername("newadmin")).thenReturn(false);
        when(userRepository.existsByEmail("admin@new.com")).thenReturn(false);
        when(passwordEncoder.encode("Password@123")).thenReturn("$2a$10$newhashedpassword");

        AppUser savedUser = new AppUser("newadmin", "admin@new.com", "$2a$10$newhashedpassword", "VIEWER", "New User");
        when(userRepository.save(any(AppUser.class))).thenReturn(savedUser);
        when(jwtService.generateToken("newadmin", "VIEWER")).thenReturn("mocked.new.jwt.token");

        RegisterRequest request = new RegisterRequest("newadmin", "admin@new.com", "Password@123", "New User", "ADMIN");
        ResponseEntity<AuthResponse> response = authController.register(request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("newadmin", response.getBody().username());
        assertEquals("VIEWER", response.getBody().role());
    }

    @Test
    @DisplayName("Verify duplicate username registration rejected with 409 Conflict")
    void testRegisterDuplicateUsernameFails() {
        when(userRepository.existsByUsername("existinguser")).thenReturn(true);

        RegisterRequest request = new RegisterRequest("existinguser", "test@recallradar.com", "Password@123", "User", "VIEWER");
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                authController.register(request)
        );

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Username is already registered"));
    }
}
