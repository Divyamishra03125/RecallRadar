package com.recallradar.recall.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UpdateSeverityRequest(
        @NotBlank(message = "Severity is required")
        @Pattern(regexp = "(?i)^(CRITICAL|HIGH|MEDIUM|LOW|UNKNOWN)$", message = "Severity must be CRITICAL, HIGH, MEDIUM, LOW, or UNKNOWN")
        String severity,

        String reason,

        String evidence,

        String classificationSource,

        String changedBy
) {
    public UpdateSeverityRequest(String severity, String reason, String changedBy) {
        this(severity, reason, null, null, changedBy);
    }
}
