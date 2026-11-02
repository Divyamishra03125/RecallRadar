package com.recallradar.security.dto;

public record AuthResponse(
        String token,
        String username,
        String email,
        String fullName,
        String role
) {}
