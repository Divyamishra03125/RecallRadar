package com.recallradar.recall.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public record RecallResponse(
        Long id,
        String sourceRecordId,
        Long productId,
        String productName,
        String title,
        String description,
        String reason,
        String severity,
        LocalDate recallDate,
        String sourceUrl,
        String status,
        String associationStatus,
        String severityEvidence,
        String severityClassificationSource,
        String sourceVerificationStatus,
        OffsetDateTime createdAt
) {
    public RecallResponse(
            Long id,
            String sourceRecordId,
            Long productId,
            String productName,
            String title,
            String description,
            String reason,
            String severity,
            LocalDate recallDate,
            String sourceUrl,
            String status,
            String associationStatus,
            OffsetDateTime createdAt
    ) {
        this(
                id,
                sourceRecordId,
                productId,
                productName,
                title,
                description,
                reason,
                severity,
                recallDate,
                sourceUrl,
                status,
                associationStatus,
                null,
                null,
                "UNAVAILABLE",
                createdAt
        );
    }

    // Backward-compatible constructor for existing tests and callers
    public RecallResponse(
            Long id,
            String sourceRecordId,
            Long productId,
            String productName,
            String title,
            String description,
            String reason,
            String severity,
            LocalDate recallDate,
            String sourceUrl,
            String status,
            OffsetDateTime createdAt
    ) {
        this(
                id,
                sourceRecordId,
                productId,
                productName,
                title,
                description,
                reason,
                severity,
                recallDate,
                sourceUrl,
                status,
                productId != null ? "LINKED" : "UNLINKED",
                createdAt
        );
    }
}
