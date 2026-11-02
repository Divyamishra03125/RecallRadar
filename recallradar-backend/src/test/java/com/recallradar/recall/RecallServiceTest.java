package com.recallradar.recall;

import com.recallradar.product.Product;
import com.recallradar.product.ProductRepository;
import com.recallradar.recall.audit.RecallSeverityAudit;
import com.recallradar.recall.audit.RecallSeverityAuditRepository;
import com.recallradar.recall.dto.AssociateProductRequest;
import com.recallradar.recall.dto.ProductCandidateMatch;
import com.recallradar.recall.dto.RecallResponse;
import com.recallradar.recall.dto.UpdateSeverityRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecallServiceTest {

    @Mock
    private RecallRepository recallRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private RecallSeverityAuditRepository severityAuditRepository;

    @InjectMocks
    private RecallService recallService;

    private Recall sampleRecall;
    private Product sampleProduct;

    @BeforeEach
    void setUp() {
        sampleProduct = new Product();
        sampleProduct.setId(10L);
        sampleProduct.setName("Galaxy S24");
        sampleProduct.setBrand("Samsung");
        sampleProduct.setModelNumber("SM-S921B");
        sampleProduct.setBatchNumber("SAM-2026");

        sampleRecall = new Recall();
        sampleRecall.setId(1L);
        sampleRecall.setTitle("Samsung Galaxy S24 battery overheating SM-S921B");
        sampleRecall.setSeverity("UNKNOWN");
        sampleRecall.setStatus("ACTIVE");
        sampleRecall.setAssociationStatus("UNLINKED");
    }

    @Test
    void testUpdateSeverityCreatesAuditTrail() {
        when(recallRepository.findById(1L)).thenReturn(Optional.of(sampleRecall));
        when(recallRepository.save(any(Recall.class))).thenReturn(sampleRecall);

        UpdateSeverityRequest request = new UpdateSeverityRequest("HIGH", "Confirmed thermal event", "admin");
        RecallResponse response = recallService.updateSeverity(1L, request);

        assertEquals("HIGH", response.severity());
        verify(severityAuditRepository).save(any(RecallSeverityAudit.class));
    }

    @Test
    void testUpdateSeverityHighWithoutEvidenceOrJustificationThrowsException() {
        when(recallRepository.findById(1L)).thenReturn(Optional.of(sampleRecall));

        // Attempting to assign HIGH without reliable evidence or justification (< 8 chars)
        UpdateSeverityRequest invalidRequest = new UpdateSeverityRequest("HIGH", "bad", "", "", "admin");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                recallService.updateSeverity(1L, invalidRequest)
        );

        assertTrue(ex.getMessage().contains("documented evidence or reliable justification"));
        verify(recallRepository, never()).save(any(Recall.class));
    }

    @Test
    void testUpdateSeverityCriticalWithEvidenceAndClassificationSourceSucceeds() {
        when(recallRepository.findById(1L)).thenReturn(Optional.of(sampleRecall));
        when(recallRepository.save(any(Recall.class))).thenReturn(sampleRecall);

        UpdateSeverityRequest request = new UpdateSeverityRequest(
                "CRITICAL",
                "Hospitalization reports confirmed burn injuries",
                "CPSC Docket #24-105: 14 verified consumer burn incidents",
                "CPSC Official Notice",
                "senior_analyst"
        );

        RecallResponse response = recallService.updateSeverity(1L, request);

        assertNotNull(response);
        assertEquals("CRITICAL", response.severity());
        assertEquals("CPSC Docket #24-105: 14 verified consumer burn incidents", response.severityEvidence());
        assertEquals("CPSC Official Notice", response.severityClassificationSource());
        verify(severityAuditRepository).save(any(RecallSeverityAudit.class));
    }

    @Test
    void testFindCandidateMatchesScoresExactModelHighest() {
        when(recallRepository.findById(1L)).thenReturn(Optional.of(sampleRecall));
        when(productRepository.findAll()).thenReturn(List.of(sampleProduct));

        List<ProductCandidateMatch> matches = recallService.findCandidateMatches(1L);

        assertNotNull(matches);
        assertFalse(matches.isEmpty());
        ProductCandidateMatch match = matches.get(0);
        assertEquals(10L, match.productId());
        assertTrue(match.confidenceScore() >= 80);
        assertTrue(match.matchReason().contains("SM-S921B"));
    }

    @Test
    void testAssociateProductUpdatesStatus() {
        when(recallRepository.findById(1L)).thenReturn(Optional.of(sampleRecall));
        when(productRepository.findById(10L)).thenReturn(Optional.of(sampleProduct));
        when(recallRepository.save(any(Recall.class))).thenReturn(sampleRecall);

        AssociateProductRequest request = new AssociateProductRequest(10L, "LINKED", "Verified against serial");
        RecallResponse response = recallService.associateProduct(1L, request);

        assertNotNull(response);
        assertEquals("LINKED", response.associationStatus());
        assertEquals(10L, response.productId());
    }
}
