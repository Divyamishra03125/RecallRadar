package com.recallradar.security;

import com.recallradar.security.dto.AnalystRegistrationRequest;
import com.recallradar.security.dto.AnalystRegistrationResponse;
import com.recallradar.security.dto.AuthResponse;
import com.recallradar.security.dto.LoginRequest;
import com.recallradar.security.dto.RegisterRequest;
import com.recallradar.user.AppUser;
import com.recallradar.user.AppUserRepository;
import com.recallradar.user.UserAuditLog;
import com.recallradar.user.UserAuditLogRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UserAuditLogRepository auditLogRepository;

    @Autowired
    public AuthController(
            AppUserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            UserAuditLogRepository auditLogRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.auditLogRepository = auditLogRepository;
    }

    // Fallback constructor for backward-compatible unit tests
    public AuthController(
            AppUserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService) {
        this(userRepository, passwordEncoder, jwtService, null);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        String identifier = request.getIdentifier();
        if (identifier.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username or email is required");
        }

        AppUser user = userRepository.findByUsernameIgnoreCaseOrEmailIgnoreCase(identifier, identifier)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password"));

        // Reject pending, rejected, or disabled accounts
        String accountStatus = user.getAccountStatus() != null ? user.getAccountStatus().toUpperCase() : (user.isEnabled() ? "ACTIVE" : "DISABLED");

        if ("PENDING_APPROVAL".equals(accountStatus)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Your Analyst access request is pending administrator approval. Please wait for an administrator to review and approve your account.");
        }

        if ("REJECTED".equals(accountStatus)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Your access request has been rejected. Please contact your organization administrator.");
        }

        if (!user.isEnabled() || "DISABLED".equals(accountStatus)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Your account is disabled. Please contact an administrator.");
        }

        // Strictly verify password using BCrypt password encoder
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password");
        }

        // Strict role-based portal verification: reject cross-portal login with HTTP 403 Forbidden
        if (request.requestedPortal() != null && !request.requestedPortal().isBlank()) {
            String portal = request.requestedPortal().trim().toUpperCase();
            String userRole = user.getRole().toUpperCase();
            if (!userRole.equals(portal)) {
                String portalName = portal.substring(0, 1) + portal.substring(1).toLowerCase();
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Your account is not authorized to access the " + portalName + " Portal. Please use the portal assigned to your account or contact your administrator.");
            }
        }

        // Record last login timestamp
        user.setLastLoginAt(OffsetDateTime.now());
        userRepository.save(user);

        String token = jwtService.generateToken(user.getUsername(), user.getRole());
        return ResponseEntity.ok(new AuthResponse(
                token,
                user.getUsername(),
                user.getEmail(),
                user.getFullName(),
                user.getRole()
        ));
    }

    @PostMapping("/register/analyst")
    public ResponseEntity<AnalystRegistrationResponse> registerAnalyst(
            @Valid @RequestBody AnalystRegistrationRequest request) {
        String username = request.username().trim();
        String email = request.email().trim().toLowerCase();

        if (!request.password().equals(request.confirmPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Passwords do not match. Please verify your password confirmation.");
        }

        if (request.password().length() < 12) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password must be at least 12 characters long.");
        }

        if (userRepository.existsByUsernameIgnoreCase(username)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username '" + username + "' is already registered. Please choose another username.");
        }

        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An account with email '" + email + "' is already registered.");
        }

        String passwordHash = passwordEncoder.encode(request.password());

        // Role is strictly assigned server-side as ANALYST with PENDING_APPROVAL status.
        // User-supplied role is completely ignored to prevent privilege escalation.
        AppUser user = new AppUser(username, email, passwordHash, "ANALYST", request.fullName().trim());
        user.setAccountStatus("PENDING_APPROVAL");
        user.setEnabled(false); // cannot log in while pending
        user.setOrganization(request.organization() != null && !request.organization().trim().isEmpty() ? request.organization().trim() : null);
        user.setDepartment(request.department() != null && !request.department().trim().isEmpty() ? request.department().trim() : null);
        user.setJobTitle(request.jobTitle() != null && !request.jobTitle().trim().isEmpty() ? request.jobTitle().trim() : null);
        user.setEmailVerified(true);
        AppUser saved = userRepository.save(user);

        if (auditLogRepository != null) {
            auditLogRepository.save(new UserAuditLog(
                    saved.getId(),
                    saved.getUsername(),
                    "APPLICANT",
                    "ANALYST_REQUEST_SUBMITTED",
                    null,
                    "PENDING_APPROVAL",
                    "Analyst access request submitted for " + saved.getEmail() + " (" + (saved.getOrganization() != null ? saved.getOrganization() : "No Organization") + ")"
            ));
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(new AnalystRegistrationResponse(
                "Your Analyst access request has been submitted successfully. Your account is awaiting administrator approval. You will be able to access the Analyst workspace after your request has been reviewed and approved.",
                saved.getUsername(),
                saved.getEmail(),
                "PENDING_APPROVAL"
        ));
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        String username = request.username().trim();
        String email = request.email().trim().toLowerCase();

        if (username.length() < 3 || username.length() > 50) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username must be between 3 and 50 characters");
        }

        if (request.password() == null || request.password().length() < 6) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password must be at least 6 characters long");
        }

        if (userRepository.existsByUsername(username)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username is already registered");
        }

        if (userRepository.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email is already registered");
        }

        // IMPORTANT SECURITY RULE: Public registration strictly defaults to VIEWER.
        // Arbitrary role selection (such as ADMIN or ANALYST) is never allowed for public sign-ups.
        String assignedRole = "VIEWER";

        String passwordHash = passwordEncoder.encode(request.password());
        AppUser user = new AppUser(
                username,
                email,
                passwordHash,
                assignedRole,
                request.fullName() != null && !request.fullName().trim().isEmpty() ? request.fullName().trim() : username
        );
        user.setAccountStatus("ACTIVE");

        AppUser saved = userRepository.save(user);
        String token = jwtService.generateToken(saved.getUsername(), saved.getRole());

        return ResponseEntity.status(HttpStatus.CREATED).body(new AuthResponse(
                token,
                saved.getUsername(),
                saved.getEmail(),
                saved.getFullName(),
                saved.getRole()
        ));
    }

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("authenticated", false, "message", "Not authenticated"));
        }

        String username = authentication.getName();
        return userRepository.findByUsername(username)
                .map(user -> {
                    Map<String, Object> map = new java.util.LinkedHashMap<>();
                    map.put("authenticated", true);
                    map.put("id", user.getId());
                    map.put("username", user.getUsername());
                    map.put("email", user.getEmail());
                    map.put("fullName", user.getFullName() != null ? user.getFullName() : user.getUsername());
                    map.put("role", user.getRole());
                    map.put("accountStatus", user.getAccountStatus() != null ? user.getAccountStatus() : (user.isEnabled() ? "ACTIVE" : "DISABLED"));
                    map.put("enabled", user.isEnabled());
                    map.put("organization", user.getOrganization() != null ? user.getOrganization() : "");
                    map.put("department", user.getDepartment() != null ? user.getDepartment() : "");
                    map.put("jobTitle", user.getJobTitle() != null ? user.getJobTitle() : "");
                    map.put("createdAt", user.getCreatedAt() != null ? user.getCreatedAt().toString() : "");
                    map.put("lastLoginAt", user.getLastLoginAt() != null ? user.getLastLoginAt().toString() : "");
                    map.put("authorities", authentication.getAuthorities());
                    return ResponseEntity.ok(map);
                })
                .orElseGet(() -> ResponseEntity.ok(Map.of(
                        "authenticated", true,
                        "username", username,
                        "role", "VIEWER"
                )));
    }
}
