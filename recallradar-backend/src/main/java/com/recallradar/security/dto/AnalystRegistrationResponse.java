package com.recallradar.security.dto;

public record AnalystRegistrationResponse(
        String message,
        String username,
        String email,
        String status
) {}
