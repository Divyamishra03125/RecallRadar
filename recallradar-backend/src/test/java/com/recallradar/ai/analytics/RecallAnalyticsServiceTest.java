package com.recallradar.ai.analytics;

import com.recallradar.ai.OpenAiService;
import com.recallradar.product.Product;
import com.recallradar.recall.Recall;
import com.recallradar.recall.RecallRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecallAnalyticsServiceTest {

    @Mock
    private RecallRepository recallRepository;

    @Mock
    private OpenAiService openAiService;

    private RecallAnalyticsService analyticsService;

    @BeforeEach
    void setUp() {
        analyticsService = new RecallAnalyticsService(recallRepository, openAiService);
    }

    @Test
    void testGetTrendsWithAiSummarySuccess() {
        when(recallRepository.count()).thenReturn(25L);
        when(recallRepository.countByStatus("ACTIVE")).thenReturn(20L);
        when(recallRepository.countByStatus("INACTIVE")).thenReturn(5L);
        when(recallRepository.countRecallsBySeverity()).thenReturn(List.of(
                new Object[]{"HIGH", 15L},
                new Object[]{"MEDIUM", 8L},
                new Object[]{"LOW", 2L}
        ));
        when(recallRepository.countRecallsByCategory()).thenReturn(List.of(
                new Object[]{"Power Tools", 14L},
                new Object[]{"Appliances", 11L}
        ));
        when(recallRepository.countRecallsByBrand()).thenReturn(List.of(
                new Object[]{"DeWalt", 9L},
                new Object[]{"Bosch", 6L}
        ));
        when(openAiService.ask(anyString(), anyString())).thenReturn("Executive safety summary: Power tools represent the highest volume.");

        RecallTrendsResponse response = analyticsService.getTrends(true);

        assertEquals(25L, response.totalRecalls());
        assertEquals(20L, response.activeRecalls());
        assertEquals(5L, response.inactiveRecalls());
        assertEquals(15L, response.severityBreakdown().get("HIGH"));
        assertEquals(14L, response.topCategories().get("Power Tools"));
        assertEquals(9L, response.topBrands().get("DeWalt"));
        assertTrue(response.aiExecutiveSummary().contains("Executive safety summary"));
        verify(openAiService).ask(anyString(), anyString());
    }

    @Test
    void testGetTrendsFallsBackGracefullyWhenAiFails() {
        when(recallRepository.count()).thenReturn(10L);
        when(recallRepository.countByStatus("ACTIVE")).thenReturn(8L);
        when(recallRepository.countByStatus("INACTIVE")).thenReturn(2L);
        when(recallRepository.countRecallsBySeverity()).thenReturn(List.<Object[]>of(new Object[]{"HIGH", 6L}));
        when(recallRepository.countRecallsByCategory()).thenReturn(List.<Object[]>of(new Object[]{"Electronics", 10L}));
        when(recallRepository.countRecallsByBrand()).thenReturn(List.<Object[]>of(new Object[]{"Acme", 10L}));
        when(openAiService.ask(anyString(), anyString())).thenThrow(new RuntimeException("AI API timeout"));

        RecallTrendsResponse response = analyticsService.getTrends(true);

        assertEquals(10L, response.totalRecalls());
        assertNotNull(response.aiExecutiveSummary());
        assertTrue(response.aiExecutiveSummary().contains("RecallRadar currently tracks 10 total recalls"));
        assertTrue(response.aiExecutiveSummary().contains("Electronics"));
    }

    @Test
    void testGetHazardClustersCategorization() {
        Recall r1 = new Recall();
        r1.setTitle("Battery fire risk");
        r1.setReason("Internal cells overheat and cause combustion");

        Recall r2 = new Recall();
        r2.setTitle("Rocker collapse");
        r2.setReason("Detachment of support frame leading to fall");

        Recall r3 = new Recall();
        r3.setTitle("Toy small parts choking hazard");
        r3.setReason("Detachable beads pose severe choking risk to infants");

        when(recallRepository.findAll()).thenReturn(List.of(r1, r2, r3));
        when(openAiService.ask(anyString(), anyString())).thenReturn("Recommended mitigations for fire and structural hazards.");

        HazardClusterResponse response = analyticsService.getHazardClusters(true);

        assertEquals(3L, response.totalAnalyzed());
        assertNotNull(response.clusters());
        assertFalse(response.clusters().isEmpty());

        HazardClusterItem fireCluster = response.clusters().stream()
                .filter(c -> c.hazardCategory().contains("Fire"))
                .findFirst()
                .orElse(null);
        assertNotNull(fireCluster);
        assertTrue(fireCluster.count() >= 1);

        assertTrue(response.aiSafetyRecommendations().contains("Recommended mitigations"));
    }

    @Test
    void testGetBrandRiskForKnownBrand() {
        Product p = new Product();
        p.setBrand("Makita");
        p.setCategory("Power Tools");

        Recall r1 = new Recall();
        r1.setStatus("ACTIVE");
        r1.setSeverity("HIGH");
        r1.setProduct(p);

        Recall r2 = new Recall();
        r2.setStatus("ACTIVE");
        r2.setSeverity("HIGH");
        r2.setProduct(p);

        when(recallRepository.findByProductBrand("Makita")).thenReturn(List.of(r1, r2));
        when(openAiService.ask(anyString(), anyString())).thenReturn("Makita brand safety report: 2 active high-severity recalls.");

        BrandRiskResponse response = analyticsService.getBrandRisk("Makita", true);

        assertEquals("Makita", response.brand());
        assertEquals(2L, response.totalRecalls());
        assertEquals(2L, response.activeRecalls());
        assertEquals("HIGH", response.riskLevel());
        assertTrue(response.affectedCategories().contains("Power Tools"));
        assertTrue(response.aiBrandAssessment().contains("Makita brand safety report"));
    }

    @Test
    void testGetBrandRiskForUnknownBrand() {
        when(recallRepository.findByProductBrand("UnknownCorp")).thenReturn(List.of());

        BrandRiskResponse response = analyticsService.getBrandRisk("UnknownCorp", true);

        assertEquals("UnknownCorp", response.brand());
        assertEquals(0L, response.totalRecalls());
        assertEquals("NO_RECORDED_RECALLS", response.riskLevel());
        verifyNoInteractions(openAiService);
    }
}
