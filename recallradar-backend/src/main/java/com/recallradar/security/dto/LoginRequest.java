package com.recallradar.security.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        String username,
        String email,

        @NotBlank(message = "Password is required")
        String password,

        String requestedPortal
) {
    public LoginRequest(String username, String password) {
        this(username, null, password, null);
    }

    public LoginRequest(String username, String email, String password) {
        this(username, email, password, null);
    }

    public String getIdentifier() {
        if (username != null && !username.trim().isEmpty()) {
            return username.trim();
        }
        if (email != null && !email.trim().isEmpty()) {
            return email.trim();
        }
        return "";
    }
}
