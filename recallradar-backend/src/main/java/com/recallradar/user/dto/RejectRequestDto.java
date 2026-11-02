package com.recallradar.user.dto;

import jakarta.validation.constraints.Size;

public record RejectRequestDto(
        @Size(max = 500, message = "Rejection reason must not exceed 500 characters")
        String rejectionReason
) {}
