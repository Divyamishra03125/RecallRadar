package com.recallradar.ai.analytics;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/ai/analytics")
public class RecallAnalyticsController {

    private final RecallAnalyticsService analyticsService;

    @Autowired
    public RecallAnalyticsController(RecallAnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/trends")
    public ResponseEntity<RecallTrendsResponse> getTrends(
            @RequestParam(name = "ai", defaultValue = "true") boolean includeAiSummary) {
        return ResponseEntity.ok(analyticsService.getTrends(includeAiSummary));
    }

    @GetMapping("/hazards")
    public ResponseEntity<HazardClusterResponse> getHazardClusters(
            @RequestParam(name = "ai", defaultValue = "true") boolean includeAiSummary) {
        return ResponseEntity.ok(analyticsService.getHazardClusters(includeAiSummary));
    }

    @GetMapping("/brand")
    public ResponseEntity<BrandRiskResponse> getBrandRisk(
            @RequestParam(name = "brand") String brand,
            @RequestParam(name = "ai", defaultValue = "true") boolean includeAiSummary) {
        return ResponseEntity.ok(analyticsService.getBrandRisk(brand, includeAiSummary));
    }
}
