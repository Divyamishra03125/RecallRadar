package com.recallradar.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UpdateUserRoleRequest(
        @NotBlank(message = "Role is required")
        @Pattern(regexp = "^(?i)(ADMIN|ANALYST|VIEWER)$", message = "Role must be ADMIN, ANALYST, or VIEWER")
        String role
) {}
