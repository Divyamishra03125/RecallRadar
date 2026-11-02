package com.recallradar.ai.analytics;

public record HazardClusterItem(
        String hazardCategory,
        long count,
        String primaryRisk,
        String typicalRemedy
) {}
