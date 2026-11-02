package com.recallradar.user.dto;

import com.recallradar.user.AppUser;
import java.time.OffsetDateTime;

public record AdminUserResponse(
        Long id,
        String username,
        String email,
        String fullName,
        String role,
        boolean enabled,
        String accountStatus,
        String organization,
        String department,
        String jobTitle,
        boolean emailVerified,
        String approvedBy,
        OffsetDateTime approvedAt,
        String rejectionReason,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        OffsetDateTime lastLoginAt
) {
    public static AdminUserResponse fromEntity(AppUser user) {
        return new AdminUserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFullName(),
                user.getRole(),
                user.isEnabled(),
                user.getAccountStatus() != null ? user.getAccountStatus() : (user.isEnabled() ? "ACTIVE" : "DISABLED"),
                user.getOrganization(),
                user.getDepartment(),
                user.getJobTitle(),
                user.isEmailVerified(),
                user.getApprovedBy(),
                user.getApprovedAt(),
                user.getRejectionReason(),
                user.getCreatedAt(),
                user.getUpdatedAt(),
                user.getLastLoginAt()
        );
    }
}
