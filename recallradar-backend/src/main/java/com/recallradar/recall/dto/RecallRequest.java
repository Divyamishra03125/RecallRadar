package com.recallradar.recall.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record RecallRequest(

        @Size(max = 150, message = "Source record ID must not exceed 150 characters")
        String sourceRecordId,

        Long productId,

        @NotBlank(message = "Recall title is required")
        @Size(max = 250, message = "Title must not exceed 250 characters")
        String title,

        String description,

        String reason,

        @NotBlank(message = "Severity is required")
        @Size(max = 30, message = "Severity must not exceed 30 characters")
        String severity,

        LocalDate recallDate,

        @NotBlank(message = "Source URL is required")
        @Pattern(regexp = "^https?://.*", message = "Source URL must be a valid HTTP or HTTPS web link")
        String sourceUrl,

        @Size(max = 30, message = "Status must not exceed 30 characters")
        String status

) {}
