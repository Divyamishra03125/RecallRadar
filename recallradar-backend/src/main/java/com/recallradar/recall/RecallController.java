package com.recallradar.recall;

import com.recallradar.recall.dto.AssociateProductRequest;
import com.recallradar.recall.dto.ProductCandidateMatch;
import com.recallradar.recall.dto.RecallRequest;
import com.recallradar.recall.dto.RecallResponse;
import com.recallradar.recall.dto.SeverityAuditResponse;
import com.recallradar.recall.dto.UpdateSeverityRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/recalls")
public class RecallController {

    private final RecallService recallService;

    public RecallController(RecallService recallService) {
        this.recallService = recallService;
    }

    // CREATE RECALL
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST')")
    public ResponseEntity<RecallResponse> create(
            @Valid @RequestBody RecallRequest request) {
        RecallResponse response = recallService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // GET RECALL BY ID
    @GetMapping("/{id}")
    public ResponseEntity<RecallResponse> getById(
            @PathVariable Long id) {
        RecallResponse response = recallService.getById(id);
        return ResponseEntity.ok(response);
    }

    // SEARCH AND FILTER RECALLS
    @GetMapping("/search")
    public ResponseEntity<Page<RecallResponse>> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String severity,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<RecallResponse> response = recallService.searchRecalls(
                keyword, severity, status, page, size);
        return ResponseEntity.ok(response);
    }

    // GET ALL RECALLS
    @GetMapping
    public ResponseEntity<Page<RecallResponse>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<RecallResponse> response = recallService.getAll(page, size);
        return ResponseEntity.ok(response);
    }

    // UPDATE RECALL
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST')")
    public ResponseEntity<RecallResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody RecallRequest request) {
        RecallResponse response = recallService.update(id, request);
        return ResponseEntity.ok(response);
    }

    // UPDATE SEVERITY WITH AUDIT TRAIL
    @PatchMapping("/{id}/severity")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST')")
    public ResponseEntity<RecallResponse> updateSeverity(
            @PathVariable Long id,
            @Valid @RequestBody UpdateSeverityRequest request) {
        RecallResponse response = recallService.updateSeverity(id, request);
        return ResponseEntity.ok(response);
    }

    // GET SEVERITY AUDIT HISTORY
    @GetMapping({ "/{id}/severity-audit", "/{id}/severity/audit" })
    public ResponseEntity<List<SeverityAuditResponse>> getSeverityAuditHistory(
            @PathVariable Long id) {
        List<SeverityAuditResponse> history = recallService.getSeverityAuditHistory(id);
        return ResponseEntity.ok(history);
    }

    // GET CANDIDATE PRODUCT MATCHES WITH CONFIDENCE SCORES
    @GetMapping({ "/{id}/candidate-matches", "/{id}/match-candidates" })
    public ResponseEntity<List<ProductCandidateMatch>> getCandidateMatches(
            @PathVariable Long id) {
        List<ProductCandidateMatch> candidates = recallService.findCandidateMatches(id);
        return ResponseEntity.ok(candidates);
    }

    // ASSOCIATE OR UNLINK PRODUCT
    @PostMapping("/{id}/associate")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST')")
    public ResponseEntity<RecallResponse> associateProduct(
            @PathVariable Long id,
            @Valid @RequestBody AssociateProductRequest request) {
        RecallResponse response = recallService.associateProduct(id, request);
        return ResponseEntity.ok(response);
    }

    // DELETE RECALL
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(
            @PathVariable Long id) {
        recallService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
