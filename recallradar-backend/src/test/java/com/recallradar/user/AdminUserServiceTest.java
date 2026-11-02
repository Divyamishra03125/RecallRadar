package com.recallradar.user;

import com.recallradar.user.dto.AdminUserResponse;
import com.recallradar.user.dto.CreateUserRequest;
import com.recallradar.user.dto.UpdateUserRequest;
import com.recallradar.user.dto.UserStatsResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceTest {

    @Mock
    private AppUserRepository userRepository;

    @Mock
    private UserAuditLogRepository auditLogRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AdminUserService adminUserService;

    private AppUser adminUser;
    private AppUser viewerUser;

    @BeforeEach
    void setUp() {
        adminUser = new AppUser("admin", "admin@recallradar.com", "$2a$10$hash", "ADMIN", "System Admin");
        adminUser.setId(1L);

        viewerUser = new AppUser("viewer", "viewer@recallradar.com", "$2a$10$hash", "VIEWER", "Viewer User");
        viewerUser.setId(2L);
    }

    @Test
    @DisplayName("Admin creates new user successfully with BCrypt hashing and audit logging")
    void testCreateUserSuccess() {
        CreateUserRequest req = new CreateUserRequest(
                "Jane Doe",
                "janedoe",
                "jane@example.com",
                "SecurePass123!",
                "SecurePass123!",
                "ANALYST",
                true
        );

        when(userRepository.existsByUsernameIgnoreCase("janedoe")).thenReturn(false);
        when(userRepository.existsByEmailIgnoreCase("jane@example.com")).thenReturn(false);
        when(passwordEncoder.encode("SecurePass123!")).thenReturn("$2a$10$hashedSecurePass");
        when(userRepository.save(any(AppUser.class))).thenAnswer(invocation -> {
            AppUser u = invocation.getArgument(0);
            u.setId(10L);
            return u;
        });

        AdminUserResponse response = adminUserService.createUser(req, "admin");

        assertNotNull(response);
        assertEquals("janedoe", response.username());
        assertEquals("jane@example.com", response.email());
        assertEquals("ANALYST", response.role());
        assertTrue(response.enabled());
        verify(passwordEncoder, times(1)).encode("SecurePass123!");
        verify(auditLogRepository, times(1)).save(any(UserAuditLog.class));
    }

    @Test
    @DisplayName("Create user fails when password and confirm password do not match")
    void testCreateUserPasswordMismatch() {
        CreateUserRequest req = new CreateUserRequest(
                "Jane Doe",
                "janedoe",
                "jane@example.com",
                "SecurePass123!",
                "DifferentPass!",
                "ANALYST",
                true
        );

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                adminUserService.createUser(req, "admin")
        );

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("confirmation does not match"));
        verify(userRepository, never()).save(any(AppUser.class));
    }

    @Test
    @DisplayName("Create user fails when username is duplicate")
    void testCreateUserDuplicateUsername() {
        CreateUserRequest req = new CreateUserRequest(
                "Jane Doe",
                "janedoe",
                "jane@example.com",
                "SecurePass123!",
                "SecurePass123!",
                "ANALYST",
                true
        );

        when(userRepository.existsByUsernameIgnoreCase("janedoe")).thenReturn(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                adminUserService.createUser(req, "admin")
        );

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        assertTrue(ex.getReason().contains("already taken"));
        verify(userRepository, never()).save(any(AppUser.class));
    }

    @Test
    @DisplayName("Create user fails when email is duplicate")
    void testCreateUserDuplicateEmail() {
        CreateUserRequest req = new CreateUserRequest(
                "Jane Doe",
                "janedoe",
                "jane@example.com",
                "SecurePass123!",
                "SecurePass123!",
                "ANALYST",
                true
        );

        when(userRepository.existsByUsernameIgnoreCase("janedoe")).thenReturn(false);
        when(userRepository.existsByEmailIgnoreCase("jane@example.com")).thenReturn(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                adminUserService.createUser(req, "admin")
        );

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        assertTrue(ex.getReason().contains("already exists"));
        verify(userRepository, never()).save(any(AppUser.class));
    }

    @Test
    @DisplayName("Admin promotes Viewer to Analyst successfully and logs audit")
    void testPromoteViewerToAnalyst() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(viewerUser));
        when(userRepository.save(any(AppUser.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AdminUserResponse response = adminUserService.updateUserRole(2L, "ANALYST", "admin");

        assertNotNull(response);
        assertEquals("ANALYST", response.role());
        verify(auditLogRepository, times(1)).save(any(UserAuditLog.class));
    }

    @Test
    @DisplayName("Prevent demoting the last active administrator")
    void testCannotDemoteLastActiveAdmin() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser));
        when(userRepository.countByRoleAndEnabledTrue("ADMIN")).thenReturn(1L);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                adminUserService.updateUserRole(1L, "VIEWER", "admin")
        );

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("cannot demote the last active administrator"));
        verify(userRepository, never()).save(any(AppUser.class));
    }

    @Test
    @DisplayName("Prevent disabling the last active administrator")
    void testCannotDisableLastActiveAdmin() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser));
        when(userRepository.countByRoleAndEnabledTrue("ADMIN")).thenReturn(1L);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                adminUserService.updateUserStatus(1L, false, "admin")
        );

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("cannot disable the last active administrator"));
        verify(userRepository, never()).save(any(AppUser.class));
    }

    @Test
    @DisplayName("Admin disables non-admin account successfully")
    void testDisableViewerAccount() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(viewerUser));
        when(userRepository.save(any(AppUser.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AdminUserResponse response = adminUserService.updateUserStatus(2L, false, "admin");

        assertNotNull(response);
        assertFalse(response.enabled());
        verify(auditLogRepository, times(1)).save(any(UserAuditLog.class));
    }

    @Test
    @DisplayName("Search users returns paginated results with role and status")
    void testSearchUsers() {
        Page<AppUser> page = new PageImpl<>(List.of(adminUser, viewerUser));
        when(userRepository.searchUsers(eq("test"), eq("ADMIN"), eq(true), isNull(), any(Pageable.class))).thenReturn(page);

        Page<AdminUserResponse> result = adminUserService.getAllUsers("test", "ADMIN", "ACTIVE", 0, 10);

        assertEquals(2, result.getTotalElements());
        assertEquals("admin", result.getContent().get(0).username());
    }

    @Test
    @DisplayName("Get user statistics returns accurate counts from PostgreSQL repository")
    void testGetUserStats() {
        when(userRepository.count()).thenReturn(10L);
        when(userRepository.countByEnabled(true)).thenReturn(8L);
        when(userRepository.countByEnabled(false)).thenReturn(2L);
        when(userRepository.countByRoleIgnoreCase("ADMIN")).thenReturn(3L);
        when(userRepository.countByRoleIgnoreCase("ANALYST")).thenReturn(4L);
        when(userRepository.countByRoleIgnoreCase("VIEWER")).thenReturn(3L);
        when(userRepository.countByAccountStatusIgnoreCase("PENDING_APPROVAL")).thenReturn(2L);

        UserStatsResponse stats = adminUserService.getUserStats();

        assertEquals(10L, stats.totalUsers());
        assertEquals(8L, stats.activeUsers());
        assertEquals(2L, stats.disabledUsers());
        assertEquals(3L, stats.adminCount());
        assertEquals(4L, stats.analystCount());
        assertEquals(3L, stats.viewerCount());
        assertEquals(2L, stats.pendingRequestsCount());
    }

    @Test
    @DisplayName("Admin approves Analyst access request successfully")
    void testApproveAnalystRequestSuccess() {
        AppUser applicant = new AppUser("applicant", "app@example.com", "$2a$10$hash", "ANALYST", "Applicant User");
        applicant.setId(5L);
        applicant.setAccountStatus("PENDING_APPROVAL");
        applicant.setEnabled(false);

        when(userRepository.findById(5L)).thenReturn(Optional.of(applicant));
        when(userRepository.save(any(AppUser.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AdminUserResponse approved = adminUserService.approveAnalystRequest(5L, "admin");

        assertNotNull(approved);
        assertEquals("ACTIVE", approved.accountStatus());
        assertTrue(approved.enabled());
        assertEquals("admin", approved.approvedBy());
        assertNotNull(approved.approvedAt());
        verify(auditLogRepository, times(1)).save(any(UserAuditLog.class));
    }

    @Test
    @DisplayName("Self-approval by administrator is prohibited")
    void testSelfApprovalProhibited() {
        AppUser adminApplicant = new AppUser("admin", "admin@recallradar.com", "$2a$10$hash", "ANALYST", "Admin User");
        adminApplicant.setId(1L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(adminApplicant));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                adminUserService.approveAnalystRequest(1L, "admin")
        );

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Self-approval prohibited"));
        verify(userRepository, never()).save(any(AppUser.class));
    }
}
