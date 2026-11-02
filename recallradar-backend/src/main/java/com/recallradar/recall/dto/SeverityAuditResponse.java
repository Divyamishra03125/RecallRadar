package com.recallradar.recall.dto;

import java.time.OffsetDateTime;

public record SeverityAuditResponse(
        Long id,
        Long recallId,
        String oldSeverity,
        String newSeverity,
        String changedBy,
        String reason,
        String evidence,
        String classificationSource,
        OffsetDateTime createdAt
) {}
