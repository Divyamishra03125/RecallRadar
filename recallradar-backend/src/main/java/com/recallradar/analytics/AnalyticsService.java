package com.recallradar.analytics;

import com.recallradar.analytics.dto.AnalyticsSummaryResponse;
import com.recallradar.product.ProductRepository;
import com.recallradar.recall.Recall;
import com.recallradar.recall.RecallRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class AnalyticsService {

    private final ProductRepository productRepository;
    private final RecallRepository recallRepository;

    public AnalyticsService(ProductRepository productRepository, RecallRepository recallRepository) {
        this.productRepository = productRepository;
        this.recallRepository = recallRepository;
    }

    public AnalyticsSummaryResponse getSummary() {
        long totalProducts = productRepository.count();
        List<Recall> allRecalls = recallRepository.findAll();
        long totalRecalls = allRecalls.size();

        long activeRecalls = 0;
        long resolvedRecalls = 0;
        long criticalRecalls = 0;
        long highRecalls = 0;
        long mediumRecalls = 0;
        long lowRecalls = 0;
        long unknownRecalls = 0;
        long linkedRecalls = 0;
        long unlinkedRecalls = 0;

        Map<String, Long> severityBreakdown = new LinkedHashMap<>();
        severityBreakdown.put("CRITICAL", 0L);
        severityBreakdown.put("HIGH", 0L);
        severityBreakdown.put("MEDIUM", 0L);
        severityBreakdown.put("LOW", 0L);
        severityBreakdown.put("UNKNOWN", 0L);

        for (Recall r : allRecalls) {
            String status = r.getStatus() != null ? r.getStatus().trim().toUpperCase() : "ACTIVE";
            if ("ACTIVE".equals(status)) {
                activeRecalls++;
            } else {
                resolvedRecalls++;
            }

            String sev = r.getSeverity() != null ? r.getSeverity().trim().toUpperCase() : "UNKNOWN";
            if ("MODERATE".equals(sev)) sev = "MEDIUM";
            switch (sev) {
                case "CRITICAL" -> criticalRecalls++;
                case "HIGH" -> highRecalls++;
                case "MEDIUM" -> mediumRecalls++;
                case "LOW" -> lowRecalls++;
                default -> {
                    sev = "UNKNOWN";
                    unknownRecalls++;
                }
            }
            severityBreakdown.put(sev, severityBreakdown.getOrDefault(sev, 0L) + 1);

            if (r.getProduct() != null || "LINKED".equalsIgnoreCase(r.getAssociationStatus())) {
                linkedRecalls++;
            } else {
                unlinkedRecalls++;
            }
        }

        Map<String, Long> categoryBreakdown = new LinkedHashMap<>();
        for (Object[] row : recallRepository.countRecallsByCategory()) {
            String cat = row[0] != null ? row[0].toString() : "Uncategorized";
            Long cnt = row[1] instanceof Number ? ((Number) row[1]).longValue() : 0L;
            categoryBreakdown.put(cat, cnt);
        }

        return new AnalyticsSummaryResponse(
                totalProducts,
                totalRecalls,
                activeRecalls,
                resolvedRecalls,
                criticalRecalls,
                highRecalls,
                mediumRecalls,
                lowRecalls,
                unknownRecalls,
                linkedRecalls,
                unlinkedRecalls,
                severityBreakdown,
                categoryBreakdown,
                OffsetDateTime.now()
        );
    }
}
