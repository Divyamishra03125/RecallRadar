package com.recallradar.ai.analytics;

import java.util.List;

public record BrandRiskResponse(
        String brand,
        long totalRecalls,
        long activeRecalls,
        String riskLevel,
        List<String> affectedCategories,
        String aiBrandAssessment
) {}
