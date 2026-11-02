package com.recallradar.ai.analytics;

import java.util.Map;

public record RecallTrendsResponse(
        long totalRecalls,
        long activeRecalls,
        long inactiveRecalls,
        Map<String, Long> severityBreakdown,
        Map<String, Long> topCategories,
        Map<String, Long> topBrands,
        String aiExecutiveSummary
) {}
