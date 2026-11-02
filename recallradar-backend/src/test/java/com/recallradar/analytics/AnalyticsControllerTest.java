package com.recallradar.analytics;

import com.recallradar.analytics.dto.AnalyticsSummaryResponse;
import com.recallradar.product.ProductRepository;
import com.recallradar.recall.Recall;
import com.recallradar.recall.RecallRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnalyticsControllerTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private RecallRepository recallRepository;

    private AnalyticsService analyticsService;
    private AnalyticsController analyticsController;

    @BeforeEach
    void setUp() {
        analyticsService = new AnalyticsService(productRepository, recallRepository);
        analyticsController = new AnalyticsController(analyticsService);
    }

    @Test
    void testGetSummary() {
        when(productRepository.count()).thenReturn(5L);

        Recall r1 = new Recall();
        r1.setId(1L);
        r1.setTitle("Recall 1");
        r1.setSeverity("CRITICAL");
        r1.setStatus("ACTIVE");
        r1.setAssociationStatus("UNLINKED");

        Recall r2 = new Recall();
        r2.setId(2L);
        r2.setTitle("Recall 2");
        r2.setSeverity("HIGH");
        r2.setStatus("RESOLVED");
        r2.setAssociationStatus("LINKED");

        when(recallRepository.findAll()).thenReturn(List.of(r1, r2));
        when(recallRepository.countRecallsByCategory()).thenReturn(List.<Object[]>of(new Object[]{"Electronics", 2L}));

        ResponseEntity<AnalyticsSummaryResponse> response = analyticsController.getSummary();
        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());

        AnalyticsSummaryResponse body = response.getBody();
        assertNotNull(body);
        assertEquals(5L, body.totalProducts());
        assertEquals(2L, body.totalRecalls());
        assertEquals(1L, body.activeRecalls());
        assertEquals(1L, body.resolvedRecalls());
        assertEquals(1L, body.criticalRecalls());
        assertEquals(1L, body.highRecalls());
        assertEquals(1L, body.linkedRecalls());
        assertEquals(1L, body.unlinkedRecalls());
        assertEquals(1L, body.severityBreakdown().get("CRITICAL"));
        assertEquals(1L, body.severityBreakdown().get("HIGH"));
        assertEquals(2L, body.categoryBreakdown().get("Electronics"));
    }
}
