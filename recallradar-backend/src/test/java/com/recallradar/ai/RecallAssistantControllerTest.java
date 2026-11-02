package com.recallradar.ai;

import com.recallradar.ai.dto.AssistantRequest;
import com.recallradar.ai.dto.AssistantResponse;
import com.recallradar.product.Product;
import com.recallradar.product.ProductRepository;
import com.recallradar.product.ProductService;
import com.recallradar.recall.Recall;
import com.recallradar.recall.RecallRepository;
import com.recallradar.recall.RecallService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecallAssistantControllerTest {

    @Mock
    private OpenAiService openAiService;

    @Mock
    private RecallRepository recallRepository;

    @Mock
    private RecallService recallService;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductService productService;

    @Spy
    private FollowUpGenerator followUpGenerator = new FollowUpGenerator();

    @InjectMocks
    private RecallAssistantController controller;

    @Test
    void testAskAssistantSuccessfulAiCall() {
        when(recallService.countRecallsByStatus(anyString()))
                .thenReturn(0L);
        when(openAiService.ask(anyString(), anyString()))
                .thenReturn("Check the model number on the label.");

        ResponseEntity<AssistantResponse> response =
                controller.askAssistant(new AssistantRequest("What should I check?"));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Check the model number on the label.", response.getBody().answer());
    }

    @Test
    void testAskAssistantFallsBackToKnowledgeBaseOnAiFailure() {
        when(recallService.countRecallsByStatus(anyString()))
                .thenReturn(0L);
        when(openAiService.ask(anyString(), anyString()))
                .thenThrow(new IllegalStateException("Gemini key rejected"));

        ResponseEntity<AssistantResponse> response =
                controller.askAssistant(new AssistantRequest(
                        "What should I check before buying a product that may have been recalled?"
                ));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().answer().contains("Model & Batch Number"));
        assertTrue(response.getBody().answer().contains("SaferProducts.gov"));
    }

    @Test
    void testAskAssistantRejectsEmptyQuestion() {
        ResponseEntity<AssistantResponse> response =
                controller.askAssistant(new AssistantRequest("   "));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Please enter a question.", response.getBody().answer());
    }

    @Test
    void testAskAssistantReturnsServiceUnavailableForRecallSpecificAiFailure() {
        when(recallService.countRecallsByStatus(anyString()))
                .thenReturn(0L);
        when(openAiService.ask(anyString(), anyString()))
                .thenThrow(HttpClientErrorException.create(
                        HttpStatusCode.valueOf(401),
                        "Unauthorized",
                        org.springframework.http.HttpHeaders.EMPTY,
                        new byte[0],
                        null));

        ResponseEntity<AssistantResponse> response =
                controller.askAssistant(new AssistantRequest(
                        "Is the Acme model 123 recalled?"
                ));

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().answer().contains(
                "valid, active Gemini API key"));
    }

    @Test
    void testAskAssistantFallsBackToDegradedDbRecordsWhenGeminiFailsWithMatchingRecalls() {
        Product product = new Product();
        product.setName("Widget 3000");
        product.setBrand("Acme");
        product.setModelNumber("WD-3000");

        Recall recall = new Recall();
        recall.setId(101L);
        recall.setTitle("Battery fire hazard");
        recall.setStatus("ACTIVE");
        recall.setReason("Risk of overheating");
        recall.setSeverity("HIGH");
        recall.setRecallDate(LocalDate.of(2025, 5, 10));
        recall.setProduct(product);

        when(recallRepository.searchRecalls(eq("widget"), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(recall)));
        when(recallService.countRecallsByStatus(anyString()))
                .thenReturn(1L);
        when(openAiService.ask(anyString(), anyString()))
                .thenThrow(new IllegalStateException("Gemini API request failed with HTTP 503"));

        ResponseEntity<AssistantResponse> response =
                controller.askAssistant(new AssistantRequest(
                        "Is the widget recalled?"
                ));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().answer().contains("temporarily unavailable"));
        assertTrue(response.getBody().answer().contains("Widget 3000"));
        assertTrue(response.getBody().answer().contains("Recall ID #101"));
    }

    @Test
    void testCheckAiHealthReturnsUp() {
        when(openAiService.checkHealth()).thenReturn(Map.of(
                "status", "UP",
                "activeModel", "gemini-1.5-flash",
                "latencyMs", 120L
        ));

        ResponseEntity<Map<String, Object>> response = controller.checkAiHealth();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("UP", response.getBody().get("status"));
    }

    @Test
    void testCheckAiHealthReturnsServiceUnavailableWhenDown() {
        when(openAiService.checkHealth()).thenReturn(Map.of(
                "status", "DOWN",
                "message", "Gemini API key is not configured."
        ));

        ResponseEntity<Map<String, Object>> response = controller.checkAiHealth();

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("DOWN", response.getBody().get("status"));
    }

    @Test
    void testExactModelNumberSearchFindsMatchingRecall() {
        Product product = new Product();
        product.setName("Cordless Drill");
        product.setBrand("DeWalt");
        product.setModelNumber("DCD771");

        Recall recall = new Recall();
        recall.setId(201L);
        recall.setTitle("Drill Chuck Detachment");
        recall.setStatus("ACTIVE");
        recall.setProduct(product);

        when(recallRepository.findByProductModelNumber(eq("DCD771")))
                .thenReturn(List.of(recall));
        when(recallService.countRecallsByStatus(anyString()))
                .thenReturn(1L);
        when(openAiService.ask(anyString(), contains("DCD771")))
                .thenReturn("Recall ID 201 applies to DeWalt model DCD771.");

        ResponseEntity<AssistantResponse> response =
                controller.askAssistant(new AssistantRequest(
                        "Is model DCD771 recalled?"
                ));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Recall ID 201 applies to DeWalt model DCD771.", response.getBody().answer());
        verify(recallRepository).findByProductModelNumber("DCD771");
    }

    @Test
    void testExactBatchNumberSearchFindsMatchingRecall() {
        Product product = new Product();
        product.setName("Infant Rocker");
        product.setBrand("BabyCo");
        product.setBatchNumber("B-440");

        Recall recall = new Recall();
        recall.setId(305L);
        recall.setTitle("Rocker buckle fault");
        recall.setStatus("ACTIVE");
        recall.setProduct(product);

        when(recallRepository.findByProductBatchNumber(eq("B-440")))
                .thenReturn(List.of(recall));
        when(recallService.countRecallsByStatus(anyString()))
                .thenReturn(1L);
        when(openAiService.ask(anyString(), contains("B-440")))
                .thenReturn("Recall ID 305 applies to batch B-440.");

        ResponseEntity<AssistantResponse> response =
                controller.askAssistant(new AssistantRequest(
                        "Check batch B-440 for recalls"
                ));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Recall ID 305 applies to batch B-440.", response.getBody().answer());
        verify(recallRepository).findByProductBatchNumber("B-440");
    }

    @Test
    void testCombinedModelAndBatchSearchFindsMatchingRecall() {
        Product product = new Product();
        product.setName("Space Heater");
        product.setModelNumber("SH-100");
        product.setBatchNumber("2024-X");

        Recall recall = new Recall();
        recall.setId(401L);
        recall.setTitle("Fire Hazard");
        recall.setStatus("ACTIVE");
        recall.setProduct(product);

        when(recallRepository.findByProductModelAndBatch(eq("SH-100"), eq("2024-X")))
                .thenReturn(List.of(recall));
        when(recallService.countRecallsByStatus(anyString()))
                .thenReturn(1L);
        when(openAiService.ask(anyString(), contains("SH-100")))
                .thenReturn("Model SH-100 batch 2024-X has an active recall.");

        ResponseEntity<AssistantResponse> response =
                controller.askAssistant(new AssistantRequest(
                        "Check model SH-100 batch 2024-X"
                ));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Model SH-100 batch 2024-X has an active recall.", response.getBody().answer());
        verify(recallRepository).findByProductModelAndBatch("SH-100", "2024-X");
    }

    @Test
    void testSpecificIdentifierReturnsEmptyContextWhenModelNotPresentInDb() {
        // Query has model XYZ-999 which does NOT exist in DB
        when(recallRepository.findByProductModelNumber(eq("XYZ-999")))
                .thenReturn(List.of());
        when(recallService.countRecallsByStatus(anyString()))
                .thenReturn(0L);
        when(openAiService.ask(anyString(), contains("No matching recall records were found")))
                .thenReturn("No matching recall records were found in the RecallRadar database.");

        ResponseEntity<AssistantResponse> response =
                controller.askAssistant(new AssistantRequest(
                        "Is model XYZ-999 recalled?"
                ));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("No matching recall records were found in the RecallRadar database.", response.getBody().answer());
    }

    @Test
    void testAskAssistantRoutesGreetingWithoutCallingAi() {
        ResponseEntity<AssistantResponse> response =
                controller.askAssistant(new AssistantRequest("Hello"));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().answer().contains("I am RecallRadar"));
        verifyNoInteractions(openAiService);
    }

    @Test
    void testAskAssistantRoutesDatabaseStatisticsWithoutCallingAi() {
        when(recallService.countRecallsByStatus("ACTIVE")).thenReturn(15L);
        when(recallService.countRecallsByStatus("INACTIVE")).thenReturn(5L);

        ResponseEntity<AssistantResponse> response =
                controller.askAssistant(new AssistantRequest(
                        "How many recalls are in the database?"
                ));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().answer().contains("Active Recalls: 15"));
        assertTrue(response.getBody().answer().contains("Inactive Recalls: 5"));
        assertTrue(response.getBody().answer().contains("Total Recalls Recorded: 20"));
        verifyNoInteractions(openAiService);
    }

    @Test
    void testAskAssistantGeneratesSuggestedFollowUpsOnGreeting() {
        ResponseEntity<AssistantResponse> response =
                controller.askAssistant(new AssistantRequest("Hello"));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().suggestedFollowUps());
        assertFalse(response.getBody().suggestedFollowUps().isEmpty());
    }

    @Test
    void testAskAssistantResolvesContextFromConversationHistory() {
        Product product = new Product();
        product.setName("Cordless Drill");
        product.setBrand("DeWalt");
        product.setModelNumber("DCD771");

        Recall recall = new Recall();
        recall.setId(201L);
        recall.setTitle("Drill Chuck Detachment");
        recall.setStatus("ACTIVE");
        recall.setReason("Risk of chuck detaching during operation.");
        recall.setProduct(product);

        when(recallRepository.findByIdWithProduct(201L))
                .thenReturn(java.util.Optional.of(recall));
        when(recallService.countRecallsByStatus(anyString()))
                .thenReturn(1L);
        when(openAiService.ask(anyString(), contains("Recall ID: 201")))
                .thenReturn("For Recall #201, contact DeWalt for a free inspection and chuck replacement.");

        // User asks anaphoric follow up without explicit identifier, but conversation history contains Recall ID #201
        AssistantRequest followUpRequest = new AssistantRequest(
                "What is the remedy for this recall?",
                List.of(
                        ChatMessage.user("Is model DCD771 recalled?"),
                        ChatMessage.assistant("Recall ID #201 applies to DeWalt model DCD771.")
                )
        );

        ResponseEntity<AssistantResponse> response =
                controller.askAssistant(followUpRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().answer().contains("free inspection and chuck replacement"));
        assertNotNull(response.getBody().suggestedFollowUps());
        assertFalse(response.getBody().suggestedFollowUps().isEmpty());
        verify(recallRepository).findByIdWithProduct(201L);
    }

    @Test
    void testAskAssistantRoutesDirectProductSearchRedmiWithoutCallingAi() {
        when(productRepository.findByBrandIgnoreCase("redmi")).thenReturn(List.of());
        when(productRepository.searchProducts("redmi")).thenReturn(List.of());

        ResponseEntity<AssistantResponse> response =
                controller.askAssistant(new AssistantRequest("Redmi"));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("PRODUCT_SEARCH", response.getBody().intent());
        assertEquals("NO_MATCH", response.getBody().matchStatus());
        assertTrue(response.getBody().answer().contains("No registered products or recall records matching"));
        assertTrue(response.getBody().answer().contains("Redmi"));
        verifyNoInteractions(openAiService);
    }

    @Test
    void testAskAssistantRoutesDirectProductSearchSamsungWithMatchingRecallsWithoutCallingAi() {
        Product p = new Product();
        p.setId(1L);
        p.setName("Samsung Galaxy S24");
        p.setBrand("Samsung");
        p.setModelNumber("SM-S921B");
        p.setBatchNumber("SAM2026-001");
        p.setCategory("Electronics");

        Recall r = new Recall();
        r.setId(2L);
        r.setTitle("Battery Overheating & Thermal Runaway Hazard");
        r.setStatus("ACTIVE");
        r.setSeverity("HIGH");
        r.setRecallDate(LocalDate.of(2026, 2, 15));
        r.setProduct(p);

        when(productRepository.findByBrandIgnoreCase("samsung")).thenReturn(List.of(p));
        lenient().when(productRepository.searchProducts("samsung")).thenReturn(List.of(p));
        when(recallRepository.findByProductId(1L)).thenReturn(List.of(r));

        ResponseEntity<AssistantResponse> response =
                controller.askAssistant(new AssistantRequest("Samsung"));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("PRODUCT_SEARCH", response.getBody().intent());
        assertEquals("MATCH_FOUND", response.getBody().matchStatus());
        assertTrue(response.getBody().answer().contains("Samsung Galaxy S24"));
        assertTrue(response.getBody().answer().contains("Recall ID #2"));
        assertFalse(response.getBody().products().isEmpty());
        assertFalse(response.getBody().recalls().isEmpty());
        verifyNoInteractions(openAiService);
    }

    @Test
    void testAskAssistantRoutesDirectRecallSearchActiveRecallsWithoutCallingAi() {
        Recall r = new Recall();
        r.setId(2L);
        r.setTitle("Active Battery Overheating");
        r.setStatus("ACTIVE");
        r.setSeverity("HIGH");
        r.setRecallDate(LocalDate.of(2026, 2, 15));

        when(recallRepository.searchRecalls(any(), any(), eq("ACTIVE"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(r)));

        ResponseEntity<AssistantResponse> response =
                controller.askAssistant(new AssistantRequest("Show active recalls"));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("RECALL_SEARCH", response.getBody().intent());
        assertEquals("MATCH_FOUND", response.getBody().matchStatus());
        assertTrue(response.getBody().answer().contains("Recall ID #2"));
        assertFalse(response.getBody().recalls().isEmpty());
        verifyNoInteractions(openAiService);
    }

    @Test
    void testAskAssistantRoutesDirectRecallSearchCriticalRecallsWithoutCallingAi() {
        Recall r = new Recall();
        r.setId(5L);
        r.setTitle("Critical Airbag Inflator Rupture");
        r.setStatus("ACTIVE");
        r.setSeverity("CRITICAL");
        r.setRecallDate(LocalDate.of(2026, 1, 10));

        when(recallRepository.searchRecalls(any(), eq("CRITICAL"), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(r)));

        ResponseEntity<AssistantResponse> response =
                controller.askAssistant(new AssistantRequest("Show critical recalls"));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().answer().contains("Recall ID #5"));
        assertFalse(response.getBody().recalls().isEmpty());
        verifyNoInteractions(openAiService);
    }

    @Test
    void testAskAssistantRoutesDirectBatchSearchWithoutCallingAi() {
        Product p = new Product();
        p.setId(1L);
        p.setName("Samsung Galaxy S24");
        p.setBatchNumber("SAM2026-001");

        Recall r = new Recall();
        r.setId(2L);
        r.setTitle("Battery Overheating");
        r.setStatus("ACTIVE");
        r.setProduct(p);

        when(productRepository.findByBatchNumberIgnoreCase("SAM2026-001")).thenReturn(List.of(p));
        when(recallRepository.findByProductId(1L)).thenReturn(List.of(r));

        ResponseEntity<AssistantResponse> response =
                controller.askAssistant(new AssistantRequest("Check batch SAM2026-001"));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("BATCH_INVESTIGATION", response.getBody().intent());
        assertTrue(response.getBody().answer().contains("Batch Traceability Investigation"));
        assertTrue(response.getBody().answer().contains("SAM2026-001"));
        verifyNoInteractions(openAiService);
    }

    @Test
    void testAskAssistantRoutesDirectModelSearchWithoutCallingAi() {
        Product p = new Product();
        p.setId(1L);
        p.setName("Samsung Galaxy S24");
        p.setModelNumber("SM-S921B");

        Recall r = new Recall();
        r.setId(2L);
        r.setTitle("Battery Overheating");
        r.setStatus("ACTIVE");
        r.setProduct(p);

        when(productRepository.findByModelNumberIgnoreCase("SM-S921B")).thenReturn(List.of(p));
        when(recallRepository.findByProductId(1L)).thenReturn(List.of(r));

        ResponseEntity<AssistantResponse> response =
                controller.askAssistant(new AssistantRequest("Find model SM-S921B"));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("PRODUCT_SEARCH", response.getBody().intent());
        assertTrue(response.getBody().answer().contains("Samsung Galaxy S24"));
        verifyNoInteractions(openAiService);
    }
}
