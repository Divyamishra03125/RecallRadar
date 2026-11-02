package com.recallradar.ai.analytics;

import java.util.List;

public record HazardClusterResponse(
        long totalAnalyzed,
        List<HazardClusterItem> clusters,
        String aiSafetyRecommendations
) {}
