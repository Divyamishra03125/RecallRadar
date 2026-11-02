package com.recallradar.ai.service;

import com.recallradar.product.Product;
import com.recallradar.product.ProductRepository;
import com.recallradar.recall.Recall;
import com.recallradar.recall.RecallRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;

@Service
@Transactional(readOnly = true)
public class ProductSearchService {

    private static final Logger log = LoggerFactory.getLogger(ProductSearchService.class);

    private final ProductRepository productRepository;
    private final RecallRepository recallRepository;

    @Autowired
    public ProductSearchService(ProductRepository productRepository, RecallRepository recallRepository) {
        this.productRepository = productRepository;
        this.recallRepository = recallRepository;
    }

    public String normalizeIdentifier(String input) {
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

    public List<Product> searchByNameExact(String name) {
        if (!StringUtils.hasText(name)) return List.of();
        return productRepository.findByNameIgnoreCase(name.trim());
    }

    public List<Product> searchByNamePartial(String name) {
        if (!StringUtils.hasText(name)) return List.of();
        return productRepository.findByNameContainingIgnoreCase(name.trim());
    }

    public List<Product> searchByBrand(String brand) {
        if (!StringUtils.hasText(brand)) return List.of();
        return productRepository.findByBrandIgnoreCase(brand.trim());
    }

    public List<Product> searchByModelNumber(String modelNumber) {
        if (!StringUtils.hasText(modelNumber)) return List.of();
        String raw = modelNumber.trim();
        String normalized = normalizeIdentifier(raw);

        List<Product> exact = productRepository.findByModelNumberIgnoreCase(raw);
        if (!exact.isEmpty()) {
            return exact;
        }

        List<Product> normalizedMatches = productRepository.findByNormalizedModelNumber(normalized);
        if (!normalizedMatches.isEmpty()) {
            return normalizedMatches;
        }

        return productRepository.searchProducts(raw);
    }

    public List<Product> searchByBatchNumber(String batchNumber) {
        if (!StringUtils.hasText(batchNumber)) return List.of();
        String raw = batchNumber.trim();
        String normalized = normalizeIdentifier(raw);

        List<Product> exact = productRepository.findByBatchNumberIgnoreCase(raw);
        if (!exact.isEmpty()) {
            return exact;
        }

        return productRepository.findByNormalizedBatchNumber(normalized);
    }

    public List<Product> searchByBrandAndModel(String brand, String modelNumber) {
        if (!StringUtils.hasText(brand) || !StringUtils.hasText(modelNumber)) return List.of();
        String rawBrand = brand.trim();
        String rawModel = modelNumber.trim();
        String normalizedModel = normalizeIdentifier(rawModel);

        List<Product> exact = productRepository.findByBrandIgnoreCaseAndModelNumberIgnoreCase(rawBrand, rawModel);
        if (!exact.isEmpty()) {
            return exact;
        }

        return productRepository.findByBrandAndModelFuzzy(rawBrand, rawModel, normalizedModel);
    }

    public List<Product> searchProducts(String query, String brand, String modelNumber, String batchNumber) {
        return searchProducts(query, null, brand, modelNumber, batchNumber);
    }

    public List<Product> searchProducts(String query, Long productId, String brand, String modelNumber, String batchNumber) {
        // Priority 1: Exact / normalized model number
        if (StringUtils.hasText(modelNumber)) {
            List<Product> modelMatches = searchByModelNumber(modelNumber);
            if (!modelMatches.isEmpty()) {
                return modelMatches;
            }
        }

        // Priority 2: Exact / normalized batch number
        if (StringUtils.hasText(batchNumber)) {
            List<Product> batchMatches = searchByBatchNumber(batchNumber);
            if (!batchMatches.isEmpty()) {
                return batchMatches;
            }
        }

        // Priority 3: Exact product ID
        if (productId != null) {
            Optional<Product> p = productRepository.findById(productId);
            if (p.isPresent()) {
                return List.of(p.get());
            }
        }

        // Priority 4: Exact normalized product name
        if (StringUtils.hasText(query)) {
            List<Product> exactName = productRepository.findByNameIgnoreCase(query.trim());
            if (exactName != null && !exactName.isEmpty()) {
                return exactName;
            }
        }

        // Priority 5: Brand + Model OR Brand-only query
        if (StringUtils.hasText(brand) && StringUtils.hasText(modelNumber)) {
            List<Product> brandModel = searchByBrandAndModel(brand, modelNumber);
            if (!brandModel.isEmpty()) {
                return brandModel;
            }
        }

        if (StringUtils.hasText(brand)) {
            String qClean = query != null ? query.trim().toLowerCase(Locale.ROOT) : "";
            String bClean = brand.trim().toLowerCase(Locale.ROOT);
            if (qClean.equals(bClean) || qClean.startsWith("all " + bClean) || qClean.contains("show " + bClean) || qClean.contains("list " + bClean)) {
                List<Product> brandMatches = searchByBrand(brand);
                if (!brandMatches.isEmpty()) {
                    return brandMatches;
                }
            }
        }

        // Priority 6: Partial matching & keyword fallback (only if higher tiers returned nothing)
        Set<Product> matched = new LinkedHashSet<>();
        if (StringUtils.hasText(query)) {
            List<Product> partial = searchByNamePartial(query);
            if (partial != null && !partial.isEmpty()) {
                matched.addAll(partial);
            }

            Set<String> searchTerms = extractSearchTerms(query);
            for (String term : searchTerms) {
                List<Product> keywordMatches = productRepository.searchProducts(term);
                if (keywordMatches != null) {
                    matched.addAll(keywordMatches);
                }
            }
        }

        if (matched.isEmpty() && StringUtils.hasText(brand)) {
            List<Product> brandMatches = searchByBrand(brand);
            if (brandMatches != null) {
                matched.addAll(brandMatches);
            }
        }

        return new ArrayList<>(matched);
    }

    public List<Recall> getAssociatedRecalls(Long productId) {
        if (productId == null) return List.of();
        return recallRepository.findByProductId(productId);
    }
}
