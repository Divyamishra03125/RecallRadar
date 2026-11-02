package com.recallradar.analytics.dto;

import java.time.OffsetDateTime;
import java.util.Map;

public record AnalyticsSummaryResponse(
        long totalProducts,
        long totalRecalls,
        long activeRecalls,
        long resolvedRecalls,
        long criticalRecalls,
        long highRecalls,
        long mediumRecalls,
        long lowRecalls,
        long unknownRecalls,
        long linkedRecalls,
        long unlinkedRecalls,
        Map<String, Long> severityBreakdown,
        Map<String, Long> categoryBreakdown,
        OffsetDateTime timestamp
) {}
