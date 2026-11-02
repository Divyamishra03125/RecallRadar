package com.recallradar.security.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AnalystRegistrationRequest(
        @NotBlank(message = "Full name is required")
        @Size(max = 150, message = "Full name must not exceed 150 characters")
        String fullName,

        @NotBlank(message = "Username is required")
        @Size(min = 3, max = 100, message = "Username must be between 3 and 100 characters")
        @Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "Username may only contain letters, numbers, dots, underscores, and hyphens")
        String username,

        @NotBlank(message = "Email address is required")
        @Email(message = "Please provide a valid corporate email address")
        @Size(max = 150, message = "Email must not exceed 150 characters")
        String email,

        @Size(max = 150, message = "Organization must not exceed 150 characters")
        String organization,

        @Size(max = 150, message = "Department must not exceed 150 characters")
        String department,

        @Size(max = 150, message = "Job title must not exceed 150 characters")
        String jobTitle,

        @NotBlank(message = "Password is required")
        @Size(min = 12, max = 100, message = "Password must be at least 12 characters long")
        String password,

        @NotBlank(message = "Confirm password is required")
        String confirmPassword,

        @AssertTrue(message = "You must accept the terms and conditions to submit an access request")
        Boolean termsAccepted,

        // Untrusted client role field: backend will ignore/override this to prevent self-promotion
        String role
) {}
