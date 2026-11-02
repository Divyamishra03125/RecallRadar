package com.recallradar.recall;

import com.recallradar.exception.DuplicateRecallException;
import com.recallradar.product.Product;
import com.recallradar.product.ProductRepository;
import com.recallradar.recall.audit.RecallSeverityAudit;
import com.recallradar.recall.audit.RecallSeverityAuditRepository;
import com.recallradar.recall.dto.AssociateProductRequest;
import com.recallradar.recall.dto.ProductCandidateMatch;
import com.recallradar.recall.dto.RecallRequest;
import com.recallradar.recall.dto.RecallResponse;
import com.recallradar.recall.dto.SeverityAuditResponse;
import com.recallradar.recall.dto.UpdateSeverityRequest;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional
public class RecallService {

    private final RecallRepository recallRepository;
    private final ProductRepository productRepository;
    private final RecallSeverityAuditRepository severityAuditRepository;

    // CREATE RECALL
    public RecallResponse create(RecallRequest request) {

        String sourceRecordId = StringUtils.hasText(request.sourceRecordId())
                ? request.sourceRecordId().trim()
                : null;

        if (sourceRecordId != null && recallRepository.existsBySourceRecordId(sourceRecordId)) {
            throw new DuplicateRecallException("A recall with this source record ID already exists.");
        }

        Recall recall = new Recall();
        mapRequestToEntity(request, recall);
        recall.setSourceRecordId(sourceRecordId);

        Recall savedRecall = recallRepository.save(recall);
        return mapEntityToResponse(savedRecall);
    }

    // GET RECALL BY ID
    @Transactional(readOnly = true)
    public RecallResponse getById(Long id) {
        Recall recall = recallRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Recall not found with ID: " + id));
        return mapEntityToResponse(recall);
    }

    // GET ALL RECALLS WITH PAGINATION
    @Transactional(readOnly = true)
    public Page<RecallResponse> getAll(int page, int size) {
        validatePagination(page, size);

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(
                        Sort.Order.desc("recallDate"),
                        Sort.Order.desc("id")
                )
        );

        return recallRepository.findAll(pageable)
                .map(this::mapEntityToResponse);
    }

    // SEARCH AND FILTER RECALLS
    @Transactional(readOnly = true)
    public Page<RecallResponse> searchRecalls(
            String keyword,
            String severity,
            String status,
            int page,
            int size) {

        validatePagination(page, size);

        String normalizedKeyword = StringUtils.hasText(keyword) ? keyword.trim() : null;
        String normalizedSeverity = normalize(severity);
        String normalizedStatus = normalize(status);

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(
                        Sort.Order.desc("recallDate"),
                        Sort.Order.desc("id")
                )
        );

        return recallRepository.searchRecalls(
                normalizedKeyword,
                normalizedSeverity,
                normalizedStatus,
                pageable
        ).map(this::mapEntityToResponse);
    }

    // UPDATE RECALL
    public RecallResponse update(Long id, RecallRequest request) {
        Recall recall = recallRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Recall not found with ID: " + id));

        String oldSeverity = recall.getSeverity();
        mapRequestToEntity(request, recall);

        if (StringUtils.hasText(request.sourceRecordId())) {
            String incomingSourceId = request.sourceRecordId().trim();
            if (!incomingSourceId.equalsIgnoreCase(recall.getSourceRecordId())
                    && recallRepository.existsBySourceRecordId(incomingSourceId)) {
                throw new DuplicateRecallException("A recall with this source record ID already exists.");
            }
            recall.setSourceRecordId(incomingSourceId);
        }

        // Audit severity change if updated via full update
        if (request.severity() != null && !request.severity().equalsIgnoreCase(oldSeverity)) {
            severityAuditRepository.save(new RecallSeverityAudit(
                    recall.getId(),
                    oldSeverity,
                    recall.getSeverity(),
                    "System/Admin",
                    "Updated via recall update form"
            ));
        }

        Recall updated = recallRepository.save(recall);
        return mapEntityToResponse(updated);
    }

    // UPDATE SEVERITY WITH AUDIT TRAIL
    public RecallResponse updateSeverity(Long id, UpdateSeverityRequest request) {
        Recall recall = recallRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Recall not found with ID: " + id));

        String normalizedNewSeverity = normalize(request.severity());
        String oldSeverity = recall.getSeverity() != null ? recall.getSeverity() : "UNKNOWN";

        // Enforce evidence requirement: Never automatically assign HIGH or CRITICAL without reliable evidence
        if ("HIGH".equalsIgnoreCase(normalizedNewSeverity) || "CRITICAL".equalsIgnoreCase(normalizedNewSeverity)) {
            boolean hasEvidence = StringUtils.hasText(request.evidence()) && request.evidence().trim().length() >= 8;
            boolean hasReason = StringUtils.hasText(request.reason()) && request.reason().trim().length() >= 8;
            if (!hasEvidence && !hasReason) {
                throw new IllegalArgumentException("Assigning HIGH or CRITICAL severity requires documented evidence or reliable justification (minimum 8 characters).");
            }
        }

        recall.setSeverity(normalizedNewSeverity);
        if (StringUtils.hasText(request.evidence())) {
            recall.setSeverityEvidence(request.evidence().trim());
        }
        if (StringUtils.hasText(request.classificationSource())) {
            recall.setSeverityClassificationSource(request.classificationSource().trim());
        }
        recallRepository.save(recall);

        String changedBy = StringUtils.hasText(request.changedBy()) ? request.changedBy().trim() : "Authorized User";
        String reason = StringUtils.hasText(request.reason()) ? request.reason().trim() : "Severity review";
        String evidence = StringUtils.hasText(request.evidence()) ? request.evidence().trim() : null;
        String source = StringUtils.hasText(request.classificationSource()) ? request.classificationSource().trim() : null;

        severityAuditRepository.save(new RecallSeverityAudit(
                id,
                oldSeverity,
                normalizedNewSeverity,
                changedBy,
                reason,
                evidence,
                source
        ));

        return mapEntityToResponse(recall);
    }

    // GET SEVERITY AUDIT HISTORY
    @Transactional(readOnly = true)
    public List<SeverityAuditResponse> getSeverityAuditHistory(Long recallId) {
        if (!recallRepository.existsById(recallId)) {
            throw new EntityNotFoundException("Recall not found with ID: " + recallId);
        }

        return severityAuditRepository.findByRecallIdOrderByCreatedAtDesc(recallId)
                .stream()
                .map(a -> new SeverityAuditResponse(
                        a.getId(),
                        a.getRecallId(),
                        a.getOldSeverity(),
                        a.getNewSeverity(),
                        a.getChangedBy(),
                        a.getReason(),
                        a.getEvidence(),
                        a.getClassificationSource(),
                        a.getCreatedAt()
                ))
                .toList();
    }

    // FIND CANDIDATE PRODUCT MATCHES WITH CONFIDENCE SCORES
    @Transactional(readOnly = true)
    public List<ProductCandidateMatch> findCandidateMatches(Long recallId) {
        Recall recall = recallRepository.findById(id(recallId))
                .orElseThrow(() -> new EntityNotFoundException("Recall not found with ID: " + recallId));

        String searchText = (
                (recall.getTitle() != null ? recall.getTitle() : "") + " " +
                (recall.getDescription() != null ? recall.getDescription() : "") + " " +
                (recall.getReason() != null ? recall.getReason() : "")
        ).toLowerCase(Locale.ROOT);

        List<Product> allProducts = productRepository.findAll();
        List<ProductCandidateMatch> matches = new ArrayList<>();

        for (Product product : allProducts) {
            int score = 0;
            List<String> reasons = new ArrayList<>();

            String pModel = product.getModelNumber() != null ? product.getModelNumber().trim().toLowerCase(Locale.ROOT) : "";
            String pBatch = product.getBatchNumber() != null ? product.getBatchNumber().trim().toLowerCase(Locale.ROOT) : "";
            String pName = product.getName() != null ? product.getName().trim().toLowerCase(Locale.ROOT) : "";
            String pBrand = product.getBrand() != null ? product.getBrand().trim().toLowerCase(Locale.ROOT) : "";
            String pCategory = product.getCategory() != null ? product.getCategory().trim().toLowerCase(Locale.ROOT) : "";

            // 1. Exact Model Number match (strongest identifier)
            if (!pModel.isEmpty() && searchText.contains(pModel)) {
                score += 50;
                reasons.add("Exact model number match (" + product.getModelNumber() + ")");
            }

            // 2. Exact Batch Number match
            if (!pBatch.isEmpty() && searchText.contains(pBatch)) {
                score += 45;
                reasons.add("Exact batch code match (" + product.getBatchNumber() + ")");
            }

            // 3. Product Name match
            if (!pName.isEmpty()) {
                if (searchText.contains(pName)) {
                    score += 35;
                    reasons.add("Product name matched in recall text (" + product.getName() + ")");
                } else {
                    String[] tokens = pName.split("\\s+");
                    int matchCount = 0;
                    for (String token : tokens) {
                        if (token.length() >= 3 && searchText.contains(token)) {
                            matchCount++;
                        }
                    }
                    if (tokens.length > 1 && (double) matchCount / tokens.length >= 0.5) {
                        score += 25;
                        reasons.add("Partial product name match (" + matchCount + "/" + tokens.length + " keywords)");
                    }
                }
            }

            // 4. Category match
            if (!pCategory.isEmpty() && searchText.contains(pCategory)) {
                score += 20;
                reasons.add("Product category match (" + product.getCategory() + ")");
            }

            // 5. Brand match
            if (!pBrand.isEmpty() && searchText.contains(pBrand)) {
                if (score > 0) {
                    score += 25;
                    reasons.add("Brand match (" + product.getBrand() + ")");
                } else {
                    score = 25;
                    reasons.add("Brand name mentioned only; requires administrator confirmation");
                }
            }

            int finalScore = Math.min(100, score);
            if (finalScore >= 20) {
                matches.add(new ProductCandidateMatch(
                        product.getId(),
                        product.getName(),
                        product.getBrand(),
                        product.getCategory(),
                        product.getModelNumber(),
                        product.getBatchNumber(),
                        finalScore,
                        String.join("; ", reasons)
                ));
            }
        }

        matches.sort(Comparator.comparingInt(ProductCandidateMatch::confidenceScore).reversed());
        return matches;
    }

    private Long id(Long recallId) {
        return recallId;
    }

    // ASSOCIATE PRODUCT WITH RECALL (LINKED, UNLINKED, PENDING_REVIEW)
    public RecallResponse associateProduct(Long recallId, AssociateProductRequest request) {
        Recall recall = recallRepository.findById(recallId)
                .orElseThrow(() -> new EntityNotFoundException("Recall not found with ID: " + recallId));

        if (request.productId() != null) {
            Product product = productRepository.findById(request.productId())
                    .orElseThrow(() -> new EntityNotFoundException("Product not found with ID: " + request.productId()));
            recall.setProduct(product);
            String status = StringUtils.hasText(request.associationStatus())
                    ? request.associationStatus().trim().toUpperCase(Locale.ROOT)
                    : "LINKED";
            recall.setAssociationStatus(status);
        } else {
            recall.setProduct(null);
            recall.setAssociationStatus("UNLINKED");
        }

        Recall saved = recallRepository.save(recall);
        return mapEntityToResponse(saved);
    }

    // DELETE RECALL
    public void delete(Long id) {
        Recall recall = recallRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Recall not found with ID: " + id));
        recallRepository.delete(recall);
    }

    // MAP REQUEST DTO TO ENTITY
    private void mapRequestToEntity(RecallRequest request, Recall recall) {
        if (request.sourceUrl() != null && !request.sourceUrl().matches("^https?://.*")) {
            throw new IllegalArgumentException("Source URL must be a valid HTTP or HTTPS web address");
        }
        recall.setTitle(request.title());
        recall.setDescription(request.description());
        recall.setReason(request.reason());
        recall.setSeverity(normalizeSeverity(request.severity()));
        recall.setRecallDate(request.recallDate());
        recall.setSourceUrl(request.sourceUrl());

        String requestedStatus = normalize(request.status());
        recall.setStatus(requestedStatus != null ? requestedStatus : "ACTIVE");

        if (request.productId() == null) {
            recall.setProduct(null);
            recall.setAssociationStatus("UNLINKED");
        } else {
            Product product = productRepository.findById(request.productId())
                    .orElseThrow(() -> new EntityNotFoundException("Product not found with ID: " + request.productId()));
            recall.setProduct(product);
            recall.setAssociationStatus("LINKED");
        }
    }

    public static String normalizeSeverity(String severity) {
        if (!StringUtils.hasText(severity)) {
            return "UNKNOWN";
        }
        String upper = severity.trim().toUpperCase(Locale.ROOT);
        return switch (upper) {
            case "CRITICAL", "HIGH", "MEDIUM", "LOW", "UNKNOWN" -> upper;
            case "MODERATE" -> "MEDIUM";
            default -> "UNKNOWN";
        };
    }

    // MAP ENTITY TO RESPONSE DTO
    private RecallResponse mapEntityToResponse(Recall recall) {
        Long productId = recall.getProduct() != null ? recall.getProduct().getId() : null;
        String productName = recall.getProduct() != null ? recall.getProduct().getName() : null;
        String verificationStatus = com.recallradar.ai.RecallResponseFormatter.getSourceVerificationStatus(recall.getSourceUrl());

        return new RecallResponse(
                recall.getId(),
                recall.getSourceRecordId(),
                productId,
                productName,
                recall.getTitle(),
                recall.getDescription(),
                recall.getReason(),
                recall.getSeverity(),
                recall.getRecallDate(),
                recall.getSourceUrl(),
                recall.getStatus(),
                recall.getAssociationStatus() != null ? recall.getAssociationStatus() : (productId != null ? "LINKED" : "UNLINKED"),
                recall.getSeverityEvidence(),
                recall.getSeverityClassificationSource(),
                verificationStatus,
                recall.getCreatedAt()
        );
    }

    // NORMALIZE FILTER VALUES
    private String normalize(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }

    @Transactional(readOnly = true)
    public long countRecallsByStatus(String status) {
        return recallRepository.countByStatus(status);
    }

    // VALIDATE PAGINATION
    private void validatePagination(int page, int size) {
        if (page < 0) {
            throw new IllegalArgumentException("Page number cannot be negative");
        }
        if (size < 1 || size > 100) {
            throw new IllegalArgumentException("Page size must be between 1 and 100");
        }
    }
}
