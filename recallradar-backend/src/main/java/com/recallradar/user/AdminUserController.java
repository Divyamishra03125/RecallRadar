package com.recallradar.user;

import com.recallradar.user.dto.AdminUserResponse;
import com.recallradar.user.dto.CreateUserRequest;
import com.recallradar.user.dto.UpdateUserRequest;
import com.recallradar.user.dto.UpdateUserRoleRequest;
import com.recallradar.user.dto.UpdateUserStatusRequest;
import com.recallradar.user.dto.UserAuditLogResponse;
import com.recallradar.user.dto.UserStatsResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    @GetMapping
    public ResponseEntity<Page<AdminUserResponse>> getAllUsers(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size) {
        return ResponseEntity.ok(adminUserService.getAllUsers(search, role, status, page, size));
    }

    @GetMapping("/stats")
    public ResponseEntity<UserStatsResponse> getUserStats() {
        return ResponseEntity.ok(adminUserService.getUserStats());
    }

    @GetMapping("/{id}")
    public ResponseEntity<AdminUserResponse> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(adminUserService.getUserById(id));
    }

    @PostMapping
    public ResponseEntity<AdminUserResponse> createUser(
            @Valid @RequestBody CreateUserRequest request,
            Authentication authentication) {
        String performedBy = authentication != null ? authentication.getName() : "ADMIN";
        return ResponseEntity.status(HttpStatus.CREATED).body(adminUserService.createUser(request, performedBy));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AdminUserResponse> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRequest request,
            Authentication authentication) {
        String performedBy = authentication != null ? authentication.getName() : "ADMIN";
        return ResponseEntity.ok(adminUserService.updateUser(id, request, performedBy));
    }

    @PatchMapping("/{id}/role")
    public ResponseEntity<AdminUserResponse> updateRole(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRoleRequest request,
            Authentication authentication) {
        String performedBy = authentication != null ? authentication.getName() : "ADMIN";
        return ResponseEntity.ok(adminUserService.updateUserRole(id, request.role(), performedBy));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<AdminUserResponse> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserStatusRequest request,
            Authentication authentication) {
        String performedBy = authentication != null ? authentication.getName() : "ADMIN";
        return ResponseEntity.ok(adminUserService.updateUserStatus(id, request.enabled(), performedBy));
    }

    @GetMapping("/{id}/audit")
    public ResponseEntity<List<UserAuditLogResponse>> getUserAuditLogs(@PathVariable Long id) {
        return ResponseEntity.ok(adminUserService.getAuditLogsForUser(id));
    }

    @GetMapping("/audit")
    public ResponseEntity<Page<UserAuditLogResponse>> getAllAuditLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(adminUserService.getAllAuditLogs(page, size));
    }
}
