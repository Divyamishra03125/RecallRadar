package com.recallradar.ai.analytics;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecallAnalyticsControllerTest {

    @Mock
    private RecallAnalyticsService analyticsService;

    @InjectMocks
    private RecallAnalyticsController controller;

    @Test
    void testGetTrendsEndpoint() {
        RecallTrendsResponse mockResponse = new RecallTrendsResponse(
                50L, 40L, 10L,
                Map.of("HIGH", 25L),
                Map.of("Electronics", 20L),
                Map.of("Samsung", 10L),
                "Executive AI summary"
        );

        when(analyticsService.getTrends(true)).thenReturn(mockResponse);

        ResponseEntity<RecallTrendsResponse> entity = controller.getTrends(true);

        assertEquals(HttpStatus.OK, entity.getStatusCode());
        assertNotNull(entity.getBody());
        assertEquals(50L, entity.getBody().totalRecalls());
        verify(analyticsService).getTrends(true);
    }

    @Test
    void testGetHazardClustersEndpoint() {
        HazardClusterResponse mockResponse = new HazardClusterResponse(
                30L,
                List.of(new HazardClusterItem("Fire", 10L, "Burn risk", "Replace unit")),
                "AI mitigations"
        );

        when(analyticsService.getHazardClusters(true)).thenReturn(mockResponse);

        ResponseEntity<HazardClusterResponse> entity = controller.getHazardClusters(true);

        assertEquals(HttpStatus.OK, entity.getStatusCode());
        assertNotNull(entity.getBody());
        assertEquals(30L, entity.getBody().totalAnalyzed());
        verify(analyticsService).getHazardClusters(true);
    }

    @Test
    void testGetBrandRiskEndpoint() {
        BrandRiskResponse mockResponse = new BrandRiskResponse(
                "DeWalt",
                5L,
                4L,
                "HIGH",
                List.of("Tools"),
                "Brand assessment"
        );

        when(analyticsService.getBrandRisk(eq("DeWalt"), anyBoolean())).thenReturn(mockResponse);

        ResponseEntity<BrandRiskResponse> entity = controller.getBrandRisk("DeWalt", true);

        assertEquals(HttpStatus.OK, entity.getStatusCode());
        assertNotNull(entity.getBody());
        assertEquals("DeWalt", entity.getBody().brand());
        assertEquals("HIGH", entity.getBody().riskLevel());
        verify(analyticsService).getBrandRisk("DeWalt", true);
    }
}
