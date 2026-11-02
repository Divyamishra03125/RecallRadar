package com.recallradar.ai.service;

import com.recallradar.product.Product;
import com.recallradar.recall.Recall;
import com.recallradar.recall.RecallRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.*;

@Service
@Transactional(readOnly = true)
public class RecallRetrievalService {

    private static final Logger log = LoggerFactory.getLogger(RecallRetrievalService.class);

    private final RecallRepository recallRepository;

    @Autowired
    public RecallRetrievalService(RecallRepository recallRepository) {
        this.recallRepository = recallRepository;
    }

    private String normalizeIdentifier(String input) {
        if (!StringUtils.hasText(input)) return "";
        return input.toLowerCase(Locale.ROOT).replaceAll("[-_\\s/.]", "");
    }

    public Set<String> extractSearchTerms(String question) {
        if (!StringUtils.hasText(question)) return Set.of();
        Set<String> terms = new LinkedHashSet<>();
        String lower = question.toLowerCase(Locale.ROOT);
        String cleaned = lower.replaceAll("(?i)\\b(does|do|is|are|the|a|an|any|recalled|recalls|recall|have|has|had|product|products|check|for|with|in|database)\\b", " ");
        String[] tokens = cleaned.split("[\\s,;:.?!]+");
        for (String token : tokens) {
            String t = token.trim();
            if (t.length() >= 3) {
                terms.add(t);
            }
        }
        return terms;
    }

    public Optional<Recall> getById(Long id) {
        if (id == null) return Optional.empty();
        return recallRepository.findByIdWithProduct(id);
    }

    public List<Recall> getByModelNumber(String modelNumber) {
        if (!StringUtils.hasText(modelNumber)) return List.of();
        String raw = modelNumber.trim();
        String normalized = normalizeIdentifier(raw);
        List<Recall> linked = recallRepository.findByProductModelNumber(raw);
        if (!linked.isEmpty()) return linked;
        return recallRepository.findByModelAcrossLinkedAndUnlinked(raw, normalized);
    }

    public List<Recall> getByBatchNumber(String batchNumber) {
        if (!StringUtils.hasText(batchNumber)) return List.of();
        String raw = batchNumber.trim();
        String normalized = normalizeIdentifier(raw);
        List<Recall> linked = recallRepository.findByProductBatchNumber(raw);
        if (!linked.isEmpty()) return linked;
        return recallRepository.findByBatchAcrossLinkedAndUnlinked(raw, normalized);
    }

    public List<Recall> getByBrand(String brand) {
        if (!StringUtils.hasText(brand)) return List.of();
        List<Recall> linked = recallRepository.findByProductBrand(brand.trim());
        if (!linked.isEmpty()) return linked;
        return recallRepository.findByBrandAcrossLinkedAndUnlinked(brand.trim());
    }

    public List<Recall> getBySeverity(String severity) {
        if (!StringUtils.hasText(severity)) return List.of();
        return recallRepository.findBySeverityIgnoreCase(severity.trim());
    }

    public List<Recall> getByStatus(String status) {
        if (!StringUtils.hasText(status)) return List.of();
        return recallRepository.findByStatusIgnoreCase(status.trim());
    }

    public List<Recall> searchRecalls(
            String keyword,
            Long recallId,
            String brand,
            String modelNumber,
            String batchNumber,
            String severity,
            String status,
            LocalDate dateFrom,
            LocalDate dateTo,
            List<Product> matchedProducts
    ) {
        Set<Long> seenIds = new HashSet<>();
        List<Recall> results = new ArrayList<>();

        // 1. Explicit Recall ID
        if (recallId != null) {
            getById(recallId).ifPresent(r -> {
                if (matchesFilters(r, severity, status, dateFrom, dateTo) && seenIds.add(r.getId())) {
                    results.add(r);
                }
            });
            return results;
        }

        // 2. Strict scoping to matched products (prevents unlinked keyword leakage)
        if (matchedProducts != null && !matchedProducts.isEmpty()) {
            for (Product p : matchedProducts) {
                List<Recall> productRecalls = recallRepository.findByProductId(p.getId());
                if (productRecalls != null) {
                    for (Recall r : productRecalls) {
                        if (matchesFilters(r, severity, status, dateFrom, dateTo) && seenIds.add(r.getId())) {
                            results.add(r);
                        }
                    }
                }
            }
            return results;
        }

        // 3. By model and batch combined (unlinked or cross-product search)
        if (StringUtils.hasText(modelNumber) && StringUtils.hasText(batchNumber)) {
            List<Recall> modelAndBatch = recallRepository.findByProductModelAndBatch(modelNumber.trim(), batchNumber.trim());
            if (modelAndBatch != null) {
                for (Recall r : modelAndBatch) {
                    if (matchesFilters(r, severity, status, dateFrom, dateTo) && seenIds.add(r.getId())) {
                        results.add(r);
                    }
                }
            }
            if (!results.isEmpty()) return results;
        }

        // 4. By explicit model number
        if (StringUtils.hasText(modelNumber)) {
            List<Recall> modelMatches = recallRepository.findByProductModelNumber(modelNumber.trim());
            if (modelMatches == null || modelMatches.isEmpty()) {
                modelMatches = getByModelNumber(modelNumber);
            }
            if (modelMatches != null) {
                for (Recall r : modelMatches) {
                    if (matchesFilters(r, severity, status, dateFrom, dateTo) && seenIds.add(r.getId())) {
                        results.add(r);
                    }
                }
            }
            if (!results.isEmpty()) return results;
        }

        // 5. By explicit batch number
        if (StringUtils.hasText(batchNumber)) {
            List<Recall> batchMatches = recallRepository.findByProductBatchNumber(batchNumber.trim());
            if (batchMatches == null || batchMatches.isEmpty()) {
                batchMatches = getByBatchNumber(batchNumber);
            }
            if (batchMatches != null) {
                for (Recall r : batchMatches) {
                    if (matchesFilters(r, severity, status, dateFrom, dateTo) && seenIds.add(r.getId())) {
                        results.add(r);
                    }
                }
            }
            if (!results.isEmpty()) return results;
        }

        // 6. By brand (across linked and unlinked notices)
        if (StringUtils.hasText(brand)) {
            List<Recall> brandMatches = recallRepository.findByProductBrand(brand.trim());
            if (brandMatches == null || brandMatches.isEmpty()) {
                brandMatches = getByBrand(brand);
            }
            if (brandMatches != null) {
                for (Recall r : brandMatches) {
                    if (matchesFilters(r, severity, status, dateFrom, dateTo) && seenIds.add(r.getId())) {
                        results.add(r);
                    }
                }
            }
        }

        // 7. Search terms across recall records
        Set<String> searchTerms = extractSearchTerms(keyword);
        for (String term : searchTerms) {
            try {
                Page<Recall> paged = recallRepository.searchRecalls(
                        term,
                        severity,
                        status,
                        PageRequest.of(0, 10, Sort.by("recallDate").descending())
                );
                if (paged != null && paged.getContent() != null) {
                    for (Recall r : paged.getContent()) {
                        if (matchesFilters(r, severity, status, dateFrom, dateTo) && seenIds.add(r.getId())) {
                            results.add(r);
                        }
                    }
                }
            } catch (Exception ignored) {}
        }

        // 8. Severity search fallback if nothing matched yet
        if (results.isEmpty() && StringUtils.hasText(severity)) {
            List<Recall> severityMatches = getBySeverity(severity);
            if (severityMatches != null) {
                for (Recall r : severityMatches) {
                    if (matchesFilters(r, null, status, dateFrom, dateTo) && seenIds.add(r.getId())) {
                        results.add(r);
                    }
                }
            }
        }

        // 9. Status search fallback if nothing matched yet
        if (results.isEmpty() && StringUtils.hasText(status)) {
            List<Recall> statusMatches = getByStatus(status);
            if (statusMatches != null) {
                for (Recall r : statusMatches) {
                    if (matchesFilters(r, severity, null, dateFrom, dateTo) && seenIds.add(r.getId())) {
                        results.add(r);
                    }
                }
            }
        }

        return results;
    }

    private boolean matchesFilters(Recall r, String severity, String status, LocalDate from, LocalDate to) {
        if (r == null) return false;
        if (StringUtils.hasText(severity) && !severity.equalsIgnoreCase(r.getSeverity())) {
            return false;
        }
        if (StringUtils.hasText(status) && !status.equalsIgnoreCase(r.getStatus())) {
            return false;
        }
        return matchesDateRange(r, from, to);
    }

    private boolean matchesDateRange(Recall r, LocalDate from, LocalDate to) {
        if (from == null && to == null) return true;
        LocalDate recallDate = r.getRecallDate();
        if (recallDate == null) return false;
        if (from != null && recallDate.isBefore(from)) return false;
        if (to != null && recallDate.isAfter(to)) return false;
        return true;
    }

    public Map<String, Object> toStructuredRecallDetails(Recall r) {
        if (r == null) return Map.of();
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", r.getId());
        map.put("sourceRecordId", r.getSourceRecordId() != null ? r.getSourceRecordId() : "Unavailable");
        map.put("title", r.getTitle() != null ? r.getTitle() : "Untitled");
        map.put("description", r.getDescription() != null ? r.getDescription() : "No description provided");
        map.put("reason", r.getReason() != null ? r.getReason() : "No reason provided");
        map.put("severity", r.getSeverity() != null ? r.getSeverity() : "UNKNOWN");
        map.put("severityEvidence", r.getSeverityEvidence() != null ? r.getSeverityEvidence() : "None documented");
        map.put("severityClassificationSource", r.getSeverityClassificationSource() != null ? r.getSeverityClassificationSource() : "Unspecified");
        map.put("status", r.getStatus() != null ? r.getStatus() : "ACTIVE");
        map.put("associationStatus", r.getAssociationStatus() != null ? r.getAssociationStatus() : "UNLINKED");
        map.put("recallDate", r.getRecallDate() != null ? r.getRecallDate().toString() : "Date unavailable");
        map.put("sourceUrl", r.getSourceUrl() != null ? r.getSourceUrl() : "Unavailable");

        Product p = r.getProduct();
        if (p != null) {
            map.put("productId", p.getId());
            map.put("productName", p.getName() != null ? p.getName() : "Unknown");
            map.put("brand", p.getBrand() != null ? p.getBrand() : "Unknown");
            map.put("category", p.getCategory() != null ? p.getCategory() : "Uncategorized");
            map.put("modelNumber", p.getModelNumber() != null ? p.getModelNumber() : "Unavailable");
            map.put("batchNumber", p.getBatchNumber() != null ? p.getBatchNumber() : "Unavailable");
        } else {
            map.put("productId", null);
            map.put("productName", "Unlinked Product");
            map.put("brand", "Unknown");
            map.put("category", "Uncategorized");
            map.put("modelNumber", "Unavailable");
            map.put("batchNumber", "Unavailable");
        }

        return map;
    }
}
