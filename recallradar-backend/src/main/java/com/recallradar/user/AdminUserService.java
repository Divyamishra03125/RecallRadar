package com.recallradar.user;

import com.recallradar.user.dto.AdminUserResponse;
import com.recallradar.user.dto.CreateUserRequest;
import com.recallradar.user.dto.UpdateUserRequest;
import com.recallradar.user.dto.UserAuditLogResponse;
import com.recallradar.user.dto.UserStatsResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.List;

@Service
public class AdminUserService {

    private static final Logger log = LoggerFactory.getLogger(AdminUserService.class);

    private final AppUserRepository userRepository;
    private final UserAuditLogRepository auditLogRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminUserService(AppUserRepository userRepository,
                            UserAuditLogRepository auditLogRepository,
                            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.auditLogRepository = auditLogRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public Page<AdminUserResponse> getAllUsers(String search, String role, String status, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.min(100, Math.max(1, size)), Sort.by("id").ascending());
        String cleanSearch = (search != null && !search.trim().isEmpty()) ? search.trim() : null;
        String cleanRole = (role != null && !role.trim().isEmpty() && !role.equalsIgnoreCase("ALL")) ? role.trim().toUpperCase() : null;

        Boolean statusFilter = null;
        if (status != null && !status.trim().isEmpty() && !status.equalsIgnoreCase("ALL")) {
            if (status.equalsIgnoreCase("ACTIVE") || status.equalsIgnoreCase("ENABLED") || status.equalsIgnoreCase("TRUE")) {
                statusFilter = true;
            } else if (status.equalsIgnoreCase("DISABLED") || status.equalsIgnoreCase("FALSE")) {
                statusFilter = false;
            }
        }

        return userRepository.searchUsers(cleanSearch, cleanRole, statusFilter, null, pageable)
                .map(AdminUserResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public UserStatsResponse getUserStats() {
        long total = userRepository.count();
        long active = userRepository.countByEnabled(true);
        long disabled = userRepository.countByEnabled(false);
        long admins = userRepository.countByRoleIgnoreCase("ADMIN");
        long analysts = userRepository.countByRoleIgnoreCase("ANALYST");
        long viewers = userRepository.countByRoleIgnoreCase("VIEWER");
        long pendingRequests = userRepository.countByAccountStatusIgnoreCase("PENDING_APPROVAL");
        return new UserStatsResponse(total, active, disabled, admins, analysts, viewers, pendingRequests);
    }

    @Transactional(readOnly = true)
    public Page<AdminUserResponse> getAnalystRequests(String status, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.min(100, Math.max(1, size)), Sort.by("id").descending());
        String cleanStatus = (status != null && !status.trim().isEmpty() && !status.equalsIgnoreCase("ALL"))
                ? status.trim().toUpperCase()
                : "PENDING_APPROVAL";
        if (cleanStatus.equalsIgnoreCase("PENDING")) {
            cleanStatus = "PENDING_APPROVAL";
        }
        return userRepository.findByAccountStatusIgnoreCaseOrderByCreatedAtDesc(cleanStatus, pageable)
                .map(AdminUserResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public AdminUserResponse getUserById(Long id) {
        AppUser user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User with ID " + id + " not found"));
        return AdminUserResponse.fromEntity(user);
    }

    @Transactional
    public AdminUserResponse createUser(CreateUserRequest request, String performedBy) {
        if (!request.password().equals(request.confirmPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password confirmation does not match the chosen password.");
        }

        if (request.password().length() < 8) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password must be at least 8 characters long.");
        }

        String role = request.role().trim().toUpperCase();
        if (!role.equals("ADMIN") && !role.equals("ANALYST") && !role.equals("VIEWER")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid role: " + role + ". Permitted roles: ADMIN, ANALYST, VIEWER");
        }

        String cleanUsername = request.username().trim();
        if (userRepository.existsByUsernameIgnoreCase(cleanUsername)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username '" + cleanUsername + "' is already taken.");
        }

        String cleanEmail = request.email().trim().toLowerCase();
        if (userRepository.existsByEmailIgnoreCase(cleanEmail)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An account with email '" + cleanEmail + "' already exists.");
        }

        boolean enabled = request.enabled() == null || request.enabled();
        String passwordHash = passwordEncoder.encode(request.password());

        AppUser user = new AppUser(cleanUsername, cleanEmail, passwordHash, role, request.fullName().trim());
        user.setEnabled(enabled);
        user.setAccountStatus(enabled ? "ACTIVE" : "DISABLED");
        user.setEmailVerified(true);
        AppUser saved = userRepository.save(user);

        UserAuditLog audit = new UserAuditLog(
                saved.getId(),
                saved.getUsername(),
                performedBy != null ? performedBy : "ADMIN",
                "USER_CREATED",
                null,
                role,
                "User account created with role " + role + " and status " + (enabled ? "ACTIVE" : "DISABLED") + " by " + performedBy
        );
        auditLogRepository.save(audit);

        log.info("Administrator '{}' created new user '{}' (ID: {}) with role '{}'", performedBy, cleanUsername, saved.getId(), role);
        return AdminUserResponse.fromEntity(saved);
    }

    @Transactional
    public AdminUserResponse approveAnalystRequest(Long userId, String approvingAdmin) {
        AppUser user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User request with ID " + userId + " not found"));

        if (user.getUsername().equalsIgnoreCase(approvingAdmin)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Self-approval prohibited: Administrators cannot approve their own access requests.");
        }

        String previousStatus = user.getAccountStatus();
        user.setAccountStatus("ACTIVE");
        user.setEnabled(true);
        user.setRole("ANALYST");
        user.setApprovedBy(approvingAdmin);
        user.setApprovedAt(OffsetDateTime.now());
        user.setUpdatedAt(OffsetDateTime.now());
        AppUser saved = userRepository.save(user);

        auditLogRepository.save(new UserAuditLog(
                saved.getId(),
                saved.getUsername(),
                approvingAdmin,
                "ANALYST_REQUEST_APPROVED",
                previousStatus,
                "ACTIVE",
                "Analyst access request approved by administrator " + approvingAdmin
        ));

        log.info("Administrator '{}' approved Analyst access request for '{}' (ID: {})", approvingAdmin, saved.getUsername(), saved.getId());
        return AdminUserResponse.fromEntity(saved);
    }

    @Transactional
    public AdminUserResponse rejectAnalystRequest(Long userId, String rejectionReason, String rejectingAdmin) {
        AppUser user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User request with ID " + userId + " not found"));

        if (user.getUsername().equalsIgnoreCase(rejectingAdmin)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Self-rejection prohibited: Administrators cannot reject their own access requests.");
        }

        String previousStatus = user.getAccountStatus();
        user.setAccountStatus("REJECTED");
        user.setEnabled(false);
        user.setApprovedBy(rejectingAdmin);
        user.setApprovedAt(OffsetDateTime.now());
        user.setRejectionReason(rejectionReason);
        user.setUpdatedAt(OffsetDateTime.now());
        AppUser saved = userRepository.save(user);

        auditLogRepository.save(new UserAuditLog(
                saved.getId(),
                saved.getUsername(),
                rejectingAdmin,
                "ANALYST_REQUEST_REJECTED",
                previousStatus,
                "REJECTED",
                "Analyst access request rejected by administrator " + rejectingAdmin +
                        (rejectionReason != null && !rejectionReason.isBlank() ? ". Reason: " + rejectionReason : "")
        ));

        log.info("Administrator '{}' rejected Analyst access request for '{}' (ID: {})", rejectingAdmin, saved.getUsername(), saved.getId());
        return AdminUserResponse.fromEntity(saved);
    }

    @Transactional
    public AdminUserResponse updateUser(Long id, UpdateUserRequest request, String performedBy) {
        AppUser user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User with ID " + id + " not found"));

        String cleanEmail = request.email().trim().toLowerCase();
        if (!user.getEmail().equalsIgnoreCase(cleanEmail)) {
            if (userRepository.existsByEmailIgnoreCase(cleanEmail)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Email address '" + cleanEmail + "' is already in use by another account.");
            }
            String prevEmail = user.getEmail();
            user.setEmail(cleanEmail);
            auditLogRepository.save(new UserAuditLog(
                    user.getId(),
                    user.getUsername(),
                    performedBy,
                    "EMAIL_CHANGE",
                    prevEmail,
                    cleanEmail,
                    "Email updated from " + prevEmail + " to " + cleanEmail + " by " + performedBy
            ));
        }

        user.setFullName(request.fullName().trim());

        if (request.role() != null && !request.role().trim().isEmpty()) {
            String newRole = request.role().trim().toUpperCase();
            if (!newRole.equals("ADMIN") && !newRole.equals("ANALYST") && !newRole.equals("VIEWER")) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid role: " + newRole + ". Permitted roles: ADMIN, ANALYST, VIEWER");
            }
            if (!user.getRole().equalsIgnoreCase(newRole)) {
                if ("ADMIN".equalsIgnoreCase(user.getRole()) && !"ADMIN".equalsIgnoreCase(newRole)) {
                    long activeAdmins = userRepository.countByRoleAndEnabledTrue("ADMIN");
                    if (activeAdmins <= 1) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                "Privilege modification prohibited: cannot demote the last active administrator.");
                    }
                }
                String prevRole = user.getRole();
                user.setRole(newRole);
                auditLogRepository.save(new UserAuditLog(
                        user.getId(),
                        user.getUsername(),
                        performedBy,
                        "ROLE_CHANGE",
                        prevRole,
                        newRole,
                        "Role changed from " + prevRole + " to " + newRole + " by " + performedBy
                ));
            }
        }

        if (request.enabled() != null && user.isEnabled() != request.enabled()) {
            boolean newEnabled = request.enabled();
            if ("ADMIN".equalsIgnoreCase(user.getRole()) && !newEnabled && user.isEnabled()) {
                long activeAdmins = userRepository.countByRoleAndEnabledTrue("ADMIN");
                if (activeAdmins <= 1) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Account modification prohibited: cannot disable the last active administrator account.");
                }
            }
            boolean prevStatus = user.isEnabled();
            user.setEnabled(newEnabled);
            user.setAccountStatus(newEnabled ? "ACTIVE" : "DISABLED");
            auditLogRepository.save(new UserAuditLog(
                    user.getId(),
                    user.getUsername(),
                    performedBy,
                    "STATUS_CHANGE",
                    prevStatus ? "ENABLED" : "DISABLED",
                    newEnabled ? "ENABLED" : "DISABLED",
                    "Account status changed to " + (newEnabled ? "ENABLED" : "DISABLED") + " by " + performedBy
            ));
        }

        if (request.password() != null && !request.password().trim().isEmpty()) {
            String rawPassword = request.password().trim();
            if (rawPassword.length() < 8) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password must be at least 8 characters long.");
            }
            user.setPasswordHash(passwordEncoder.encode(rawPassword));
            auditLogRepository.save(new UserAuditLog(
                    user.getId(),
                    user.getUsername(),
                    performedBy,
                    "PASSWORD_RESET",
                    null,
                    null,
                    "Password was reset by administrator " + performedBy
            ));
        }

        user.setUpdatedAt(OffsetDateTime.now());
        AppUser saved = userRepository.save(user);

        log.info("Administrator '{}' updated user '{}' (ID: {})", performedBy, saved.getUsername(), saved.getId());
        return AdminUserResponse.fromEntity(saved);
    }

    @Transactional
    public AdminUserResponse updateUserRole(Long id, String newRoleInput, String performedBy) {
        String newRole = newRoleInput.trim().toUpperCase();
        if (!newRole.equals("ADMIN") && !newRole.equals("ANALYST") && !newRole.equals("VIEWER")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid role: " + newRole + ". Permitted roles: ADMIN, ANALYST, VIEWER");
        }

        AppUser user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User with ID " + id + " not found"));

        String previousRole = user.getRole();
        if (previousRole.equalsIgnoreCase(newRole)) {
            return AdminUserResponse.fromEntity(user);
        }

        if ("ADMIN".equalsIgnoreCase(previousRole) && !"ADMIN".equalsIgnoreCase(newRole)) {
            long activeAdmins = userRepository.countByRoleAndEnabledTrue("ADMIN");
            if (activeAdmins <= 1) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Privilege modification prohibited: cannot demote the last active administrator.");
            }
        }

        user.setRole(newRole);
        user.setUpdatedAt(OffsetDateTime.now());
        AppUser saved = userRepository.save(user);

        UserAuditLog audit = new UserAuditLog(
                saved.getId(),
                saved.getUsername(),
                performedBy != null ? performedBy : "ADMIN",
                "ROLE_CHANGE",
                previousRole,
                newRole,
                "Role changed from " + previousRole + " to " + newRole + " by " + performedBy
        );
        auditLogRepository.save(audit);

        log.info("User ID {} ({}) role changed from {} to {} by {}", saved.getId(), saved.getUsername(), previousRole, newRole, performedBy);
        return AdminUserResponse.fromEntity(saved);
    }

    @Transactional
    public AdminUserResponse updateUserStatus(Long id, boolean enabled, String performedBy) {
        AppUser user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User with ID " + id + " not found"));

        boolean previousStatus = user.isEnabled();
        if (previousStatus == enabled) {
            return AdminUserResponse.fromEntity(user);
        }

        if ("ADMIN".equalsIgnoreCase(user.getRole()) && !enabled && previousStatus) {
            long activeAdmins = userRepository.countByRoleAndEnabledTrue("ADMIN");
            if (activeAdmins <= 1) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Account modification prohibited: cannot disable the last active administrator account.");
            }
        }

        user.setEnabled(enabled);
        user.setAccountStatus(enabled ? "ACTIVE" : "DISABLED");
        user.setUpdatedAt(OffsetDateTime.now());
        AppUser saved = userRepository.save(user);

        UserAuditLog audit = new UserAuditLog(
                saved.getId(),
                saved.getUsername(),
                performedBy != null ? performedBy : "ADMIN",
                "STATUS_CHANGE",
                previousStatus ? "ENABLED" : "DISABLED",
                enabled ? "ENABLED" : "DISABLED",
                "Account status set to " + (enabled ? "ENABLED" : "DISABLED") + " by " + performedBy
        );
        auditLogRepository.save(audit);

        log.info("User ID {} ({}) enabled status changed from {} to {} by {}", saved.getId(), saved.getUsername(), previousStatus, enabled, performedBy);
        return AdminUserResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public List<UserAuditLogResponse> getAuditLogsForUser(Long userId) {
        return auditLogRepository.findByTargetUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(UserAuditLogResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<UserAuditLogResponse> getAllAuditLogs(int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.min(100, Math.max(1, size)));
        return auditLogRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(UserAuditLogResponse::fromEntity);
    }
}
