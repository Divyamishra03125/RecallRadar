package com.recallradar.ai.analytics;

import com.recallradar.ai.OpenAiService;
import com.recallradar.recall.Recall;
import com.recallradar.recall.RecallRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class RecallAnalyticsService {

    private static final Logger log = LoggerFactory.getLogger(RecallAnalyticsService.class);

    private final RecallRepository recallRepository;
    private final OpenAiService openAiService;

    @Autowired
    public RecallAnalyticsService(RecallRepository recallRepository, OpenAiService openAiService) {
        this.recallRepository = recallRepository;
        this.openAiService = openAiService;
    }

    public RecallTrendsResponse getTrends(boolean includeAiSummary) {
        long total = recallRepository.count();
        long active = recallRepository.countByStatus("ACTIVE");
        long inactive = recallRepository.countByStatus("INACTIVE");

        Map<String, Long> severityBreakdown = new LinkedHashMap<>();
        for (Object[] row : recallRepository.countRecallsBySeverity()) {
            if (row != null && row.length >= 2 && row[0] != null) {
                severityBreakdown.put(row[0].toString(), ((Number) row[1]).longValue());
            }
        }

        Map<String, Long> topCategories = new LinkedHashMap<>();
        for (Object[] row : recallRepository.countRecallsByCategory()) {
            if (row != null && row.length >= 2 && row[0] != null) {
                topCategories.put(row[0].toString(), ((Number) row[1]).longValue());
            }
        }

        Map<String, Long> topBrands = new LinkedHashMap<>();
        for (Object[] row : recallRepository.countRecallsByBrand()) {
            if (row != null && row.length >= 2 && row[0] != null) {
                topBrands.put(row[0].toString(), ((Number) row[1]).longValue());
            }
        }

        String aiSummary = "Analytics summary generated from database counts.";
        if (includeAiSummary) {
            aiSummary = generateTrendsAiSummary(total, active, inactive, severityBreakdown, topCategories, topBrands);
        }

        return new RecallTrendsResponse(
                total,
                active,
                inactive,
                severityBreakdown,
                topCategories,
                topBrands,
                aiSummary
        );
    }

    public HazardClusterResponse getHazardClusters(boolean includeAiSummary) {
        List<Recall> allRecalls = recallRepository.findAll();
        long totalAnalyzed = allRecalls.size();

        long fireCount = 0;
        long chokingCount = 0;
        long mechanicalCount = 0;
        long lacerationCount = 0;
        long electricCount = 0;

        for (Recall r : allRecalls) {
            String text = (Objects.toString(r.getReason(), "") + " "
                    + Objects.toString(r.getDescription(), "") + " "
                    + Objects.toString(r.getTitle(), "")).toLowerCase(Locale.ROOT);

            if (text.contains("fire") || text.contains("burn") || text.contains("overheat") || text.contains("battery") || text.contains("combustion") || text.contains("flame")) {
                fireCount++;
            }
            if (text.contains("chok") || text.contains("swallow") || text.contains("ingest") || text.contains("small part") || text.contains("suffocat")) {
                chokingCount++;
            }
            if (text.contains("detach") || text.contains("collaps") || text.contains("fall") || text.contains("break") || text.contains("fractur") || text.contains("buckle") || text.contains("tip-over")) {
                mechanicalCount++;
            }
            if (text.contains("lacerat") || text.contains("cut") || text.contains("sharp") || text.contains("blade") || text.contains("puncture")) {
                lacerationCount++;
            }
            if (text.contains("shock") || text.contains("electr") || text.contains("electrocution") || text.contains("lead") || text.contains("toxic")) {
                electricCount++;
            }
        }

        List<HazardClusterItem> clusters = new ArrayList<>();
        clusters.add(new HazardClusterItem("Fire & Thermal Hazards", fireCount, "Risk of fire, burns, or overheating components", "Immediate cessation of use and battery/power supply replacement"));
        clusters.add(new HazardClusterItem("Mechanical & Structural Failures", mechanicalCount, "Detachment of moving parts, collapse, or fall hazards", "Inspection and free manufacturer repair or reinforced bracket"));
        clusters.add(new HazardClusterItem("Choking & Ingestion Risks", chokingCount, "Small detachable parts posing suffocation risk to children", "Product return or replacement with child-safe components"));
        clusters.add(new HazardClusterItem("Laceration & Sharp Edges", lacerationCount, "Exposed blades, splintering plastic, or sharp edges", "Protective guard retrofit or full consumer refund"));
        clusters.add(new HazardClusterItem("Electrical & Chemical Hazards", electricCount, "Shock risk, insulation failure, or toxic material exposure", "Authorized technician service or immediate product exchange"));

        // Sort descending by count
        clusters.sort((a, b) -> Long.compare(b.count(), a.count()));

        String aiRecommendations = generateHazardAiRecommendations(clusters, totalAnalyzed, includeAiSummary);

        return new HazardClusterResponse(totalAnalyzed, clusters, aiRecommendations);
    }

    public BrandRiskResponse getBrandRisk(String brand, boolean includeAiSummary) {
        if (!StringUtils.hasText(brand)) {
            return new BrandRiskResponse(
                    "Unknown",
                    0,
                    0,
                    "INSUFFICIENT_DATA",
                    List.of(),
                    "Please specify a valid brand name to evaluate risk profile."
            );
        }

        String cleanBrand = brand.trim();
        List<Recall> brandRecalls = recallRepository.findByProductBrand(cleanBrand);

        if (brandRecalls.isEmpty()) {
            return new BrandRiskResponse(
                    cleanBrand,
                    0,
                    0,
                    "NO_RECORDED_RECALLS",
                    List.of(),
                    "No recalls for brand '" + cleanBrand + "' were found in the RecallRadar database. Note that absence of records does not guarantee zero product risk."
            );
        }

        long total = brandRecalls.size();
        long active = brandRecalls.stream().filter(r -> "ACTIVE".equalsIgnoreCase(r.getStatus())).count();
        long highSeverity = brandRecalls.stream().filter(r -> "HIGH".equalsIgnoreCase(r.getSeverity()) || "CRITICAL".equalsIgnoreCase(r.getSeverity())).count();

        String riskLevel;
        if (highSeverity >= 2 || active >= 3) {
            riskLevel = "HIGH";
        } else if (active >= 1 || highSeverity >= 1) {
            riskLevel = "MODERATE";
        } else {
            riskLevel = "LOW";
        }

        List<String> affectedCategories = brandRecalls.stream()
                .filter(r -> r.getProduct() != null && StringUtils.hasText(r.getProduct().getCategory()))
                .map(r -> r.getProduct().getCategory().trim())
                .distinct()
                .collect(Collectors.toList());

        String aiAssessment = generateBrandAiAssessment(cleanBrand, total, active, highSeverity, affectedCategories, includeAiSummary);

        return new BrandRiskResponse(
                cleanBrand,
                total,
                active,
                riskLevel,
                affectedCategories,
                aiAssessment
        );
    }

    private String generateTrendsAiSummary(
            long total,
            long active,
            long inactive,
            Map<String, Long> severity,
            Map<String, Long> categories,
            Map<String, Long> brands) {

        String prompt = "Provide a professional, 2-3 paragraph executive summary of these recall analytics. Highlight high-risk categories and actionable advice for consumers.";
        String context = "DATABASE RECALL METRICS:\n"
                + "Total recalls: " + total + "\n"
                + "Active recalls: " + active + "\n"
                + "Inactive recalls: " + inactive + "\n"
                + "Severity distribution: " + severity + "\n"
                + "Top affected product categories: " + categories + "\n"
                + "Top brands: " + brands + "\n";

        try {
            return openAiService.ask(prompt, context);
        } catch (Exception ex) {
            log.warn("Gemini unavailable for trends narrative; using deterministic summary: {}", ex.getMessage());
            long high = severity.getOrDefault("HIGH", 0L);
            return "RecallRadar currently tracks " + total + " total recalls (" + active + " active, " + inactive + " resolved). "
                    + "There are " + high + " recalls classified with HIGH severity. "
                    + "Leading product categories include " + String.join(", ", categories.keySet().stream().limit(3).toList()) + ". "
                    + "Consumers should actively inspect serial and batch numbers on products in high-risk categories.";
        }
    }

    private String generateHazardAiRecommendations(List<HazardClusterItem> clusters, long total, boolean includeAiSummary) {
        if (!includeAiSummary) {
            return "Consumers should stop using products with active thermal or structural detachment notices immediately and contact the manufacturer for remedy.";
        }

        String prompt = "Analyze these hazard clusters and provide concise, prioritized safety recommendations for consumer product safety and risk prevention.";
        StringBuilder context = new StringBuilder("HAZARD CLUSTERS (Total " + total + " recalls analyzed):\n");
        for (HazardClusterItem item : clusters) {
            context.append("• ").append(item.hazardCategory()).append(": ")
                    .append(item.count()).append(" notices | Risk: ").append(item.primaryRisk())
                    .append(" | Typical remedy: ").append(item.typicalRemedy()).append("\n");
        }

        try {
            return openAiService.ask(prompt, context.toString());
        } catch (Exception ex) {
            log.warn("Gemini unavailable for hazard recommendations; using deterministic guidance: {}", ex.getMessage());
            return "Prioritized Hazard Mitigation:\n"
                    + "1. Fire & Thermal: Unplug and discontinue use immediately. Do not attempt self-repair on lithium-ion batteries or heating elements.\n"
                    + "2. Mechanical Detachment: Check joints, buckles, and safety guards before each use.\n"
                    + "3. Choking Hazards: Keep affected baby and child products away from children and claim manufacturer replacement parts.\n"
                    + "4. Laceration: Inspect blades and protective shields for cracks or loosening.";
        }
    }

    private String generateBrandAiAssessment(String brand, long total, long active, long highSeverity, List<String> categories, boolean includeAiSummary) {
        if (!includeAiSummary) {
            return "Brand '" + brand + "' has " + total + " recall(s) recorded (" + active + " active, " + highSeverity + " high-severity).";
        }

        String prompt = "Evaluate the safety record of brand '" + brand + "' based on the provided recall data and give constructive consumer guidance.";
        String context = "BRAND SAFETY PROFILE:\n"
                + "Brand: " + brand + "\n"
                + "Total recalls: " + total + "\n"
                + "Active recalls: " + active + "\n"
                + "High-severity recalls: " + highSeverity + "\n"
                + "Affected categories: " + categories + "\n";

        try {
            return openAiService.ask(prompt, context);
        } catch (Exception ex) {
            log.warn("Gemini unavailable for brand assessment; using deterministic fallback: {}", ex.getMessage());
            return "Brand '" + brand + "' has " + total + " recall notice(s) on record in RecallRadar, with "
                    + active + " currently active and " + highSeverity + " categorized as HIGH severity. "
                    + "Affected product lines include: " + String.join(", ", categories) + ". "
                    + "Owners of these products should check their exact model and batch numbers against official recall notices.";
        }
    }
}
