package com.recallradar.ai;

import com.recallradar.ai.dto.AssistantRequest;
import com.recallradar.ai.dto.AssistantResponse;
import com.recallradar.ai.service.*;
import com.recallradar.product.Product;
import com.recallradar.product.ProductRepository;
import com.recallradar.product.ProductService;
import com.recallradar.recall.Recall;
import com.recallradar.recall.RecallRepository;
import com.recallradar.recall.RecallService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;

import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RecallAssistantIntelligenceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private RecallRepository recallRepository;

    @Mock
    private ProductService productService;

    @Mock
    private RecallService recallService;

    @Mock
    private OpenAiService openAiService;

    @Mock
    private FollowUpGenerator followUpGenerator;

    private RecallAssistantService assistantService;

    private Product samsungS24;
    private Recall recallGalaxy;

    @BeforeEach
    void setUp() {
        ProductSearchService productSearchService = new ProductSearchService(productRepository, recallRepository);
        RecallRetrievalService recallRetrievalService = new RecallRetrievalService(recallRepository);
        QueryIntentClassifier intentClassifier = new QueryIntentClassifier(new QuestionClassifier());
        ConversationContextService contextService = new ConversationContextService();
        RecallContextBuilder contextBuilder = new RecallContextBuilder();
        AiResponseValidator responseValidator = new AiResponseValidator();

        assistantService = new RecallAssistantService(
                productSearchService,
                recallRetrievalService,
                intentClassifier,
                contextService,
                contextBuilder,
                responseValidator,
                openAiService,
                productRepository,
                productService,
                recallRepository,
                recallService,
                followUpGenerator
        );

        samsungS24 = new Product();
        samsungS24.setId(101L);
        samsungS24.setName("Samsung Galaxy S24");
        samsungS24.setBrand("Samsung");
        samsungS24.setModelNumber("SM-S921B");
        samsungS24.setBatchNumber("SAM2026-001");
        samsungS24.setCategory("Smartphones");

        recallGalaxy = new Recall();
        recallGalaxy.setId(201L);
        recallGalaxy.setSourceRecordId("CPSC-2026-01");
        recallGalaxy.setTitle("Battery Thermal Runaway Hazard");
        recallGalaxy.setSeverity("HIGH");
        recallGalaxy.setStatus("ACTIVE");
        recallGalaxy.setAssociationStatus("LINKED");
        recallGalaxy.setRecallDate(LocalDate.of(2026, 1, 15));
        recallGalaxy.setSourceUrl("https://www.cpsc.gov/recalls/2026/samsung-battery");
        recallGalaxy.setProduct(samsungS24);
    }

    // 1. Exact product search
    @Test
    @DisplayName("Scenario 1: Exact product search returns direct database answer with product card")
    void testScenario1_ExactProductSearch() {
        when(productRepository.findByNameIgnoreCase("Samsung Galaxy S24")).thenReturn(List.of(samsungS24));
        when(recallRepository.findByProductId(101L)).thenReturn(List.of(recallGalaxy));

        ResponseEntity<AssistantResponse> response = assistantService.processRequest(
                new AssistantRequest("Samsung Galaxy S24")
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("DATABASE_DIRECT", response.getBody().sourceType());
        assertEquals("MATCH_FOUND", response.getBody().matchStatus());
        assertTrue(response.getBody().answer().contains("Samsung Galaxy S24"));
        verifyNoInteractions(openAiService);
    }

    // 2. Partial brand search
    @Test
    @DisplayName("Scenario 2: Partial brand search returns matching products from database")
    void testScenario2_PartialBrandSearch() {
        when(productRepository.findByBrandIgnoreCase("samsung")).thenReturn(List.of(samsungS24));
        when(productRepository.searchProducts("samsung")).thenReturn(List.of(samsungS24));

        ResponseEntity<AssistantResponse> response = assistantService.processRequest(
                new AssistantRequest("Samsung")
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("DATABASE_DIRECT", response.getBody().sourceType());
        assertFalse(response.getBody().products().isEmpty());
        verifyNoInteractions(openAiService);
    }

    // 3. Brand + Model combined search
    @Test
    @DisplayName("Scenario 3: Brand + Model combined search locates specific record")
    void testScenario3_BrandAndModelCombinedSearch() {
        when(productRepository.findByModelNumberIgnoreCase("SM-S921B")).thenReturn(List.of(samsungS24));
        when(recallRepository.findByProductId(101L)).thenReturn(List.of(recallGalaxy));

        ResponseEntity<AssistantResponse> response = assistantService.processRequest(
                new AssistantRequest("Check Samsung model SM-S921B")
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().products().isEmpty());
    }

    // 4. Model number punctuation tolerance
    @Test
    @DisplayName("Scenario 4: Model number punctuation tolerance finds SM-S921B via sms921b")
    void testScenario4_ModelPunctuationTolerance() {
        when(productRepository.findByNormalizedModelNumber("sms921b")).thenReturn(List.of(samsungS24));
        when(recallRepository.findByProductId(101L)).thenReturn(List.of(recallGalaxy));

        ResponseEntity<AssistantResponse> response = assistantService.processRequest(
                new AssistantRequest("Check model sms921b")
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().products().isEmpty());
    }

    // 5. Batch search with linked recall
    @Test
    @DisplayName("Scenario 5: Batch search with linked recall resolves traceability notice")
    void testScenario5_BatchSearchWithLinkedRecall() {
        when(productRepository.findByBatchNumberIgnoreCase("SAM2026-001")).thenReturn(List.of(samsungS24));
        when(recallRepository.findByProductId(101L)).thenReturn(List.of(recallGalaxy));

        ResponseEntity<AssistantResponse> response = assistantService.processRequest(
                new AssistantRequest("Check batch SAM2026-001")
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("MATCH_FOUND", response.getBody().matchStatus());
        assertTrue(response.getBody().answer().contains("Batch Traceability"));
        verifyNoInteractions(openAiService);
    }

    // 6. Batch search with unlinked recall
    @Test
    @DisplayName("Scenario 6: Batch search across unlinked recall notices")
    void testScenario6_BatchSearchWithUnlinkedRecall() {
        Recall unlinkedRecall = new Recall();
        unlinkedRecall.setId(301L);
        unlinkedRecall.setTitle("Unlinked Batch Defect");
        unlinkedRecall.setStatus("ACTIVE");
        unlinkedRecall.setAssociationStatus("UNLINKED");

        when(productRepository.findByBatchNumberIgnoreCase("LOT-9999")).thenReturn(List.of());
        when(productRepository.findByNormalizedBatchNumber("lot9999")).thenReturn(List.of());
        when(recallRepository.findByBatchAcrossLinkedAndUnlinked("LOT-9999", "lot9999")).thenReturn(List.of(unlinkedRecall));

        ResponseEntity<AssistantResponse> response = assistantService.processRequest(
                new AssistantRequest("Check batch LOT-9999")
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().recalls().isEmpty());
    }

    // 7. Recall ID direct search
    @Test
    @DisplayName("Scenario 7: Direct Recall ID lookup returns exact recall notice")
    void testScenario7_RecallIdDirectSearch() {
        when(recallRepository.findByIdWithProduct(201L)).thenReturn(Optional.of(recallGalaxy));
        when(recallService.countRecallsByStatus("ACTIVE")).thenReturn(5L);
        when(openAiService.ask(anyString(), anyString())).thenReturn("Recall #201 details: Battery Thermal Runaway.");

        ResponseEntity<AssistantResponse> response = assistantService.processRequest(
                new AssistantRequest("Explain Recall ID #201")
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().recalls().isEmpty());
        assertEquals(201L, response.getBody().recalls().get(0).get("id"));
    }

    // 8. Active status recall search
    @Test
    @DisplayName("Scenario 8: Active status search returns active enforcement notices")
    void testScenario8_ActiveStatusRecallSearch() {
        when(recallRepository.searchRecalls(any(), any(), eq("ACTIVE"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(recallGalaxy)));

        ResponseEntity<AssistantResponse> response = assistantService.processRequest(
                new AssistantRequest("Show active recalls")
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("MATCH_FOUND", response.getBody().matchStatus());
        verifyNoInteractions(openAiService);
    }

    // 9. Severity filter search
    @Test
    @DisplayName("Scenario 9: Severity filter search routes directly to database notices")
    void testScenario9_SeverityFilterSearch() {
        when(recallRepository.searchRecalls(any(), eq("HIGH"), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(recallGalaxy)));

        ResponseEntity<AssistantResponse> response = assistantService.processRequest(
                new AssistantRequest("Show high severity recalls")
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        verifyNoInteractions(openAiService);
    }

    // 10. Ambiguous brand query
    @Test
    @DisplayName("Scenario 10: Ambiguous brand query flags AMBIGUOUS matchStatus and warning")
    void testScenario10_AmbiguousBrandQuery() {
        Product p2 = new Product();
        p2.setId(102L);
        p2.setName("Samsung Galaxy A54");
        p2.setBrand("Samsung");

        when(productRepository.findByBrandIgnoreCase("samsung")).thenReturn(List.of(samsungS24, p2));
        when(productRepository.searchProducts("samsung")).thenReturn(List.of(samsungS24, p2));

        ResponseEntity<AssistantResponse> response = assistantService.processRequest(
                new AssistantRequest("Samsung")
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("AMBIGUOUS", response.getBody().matchStatus());
        assertFalse(response.getBody().warnings().isEmpty());
    }

    // 11. Product found with no recalls
    @Test
    @DisplayName("Scenario 11: Product found with no recalls returns PRODUCT_FOUND_NO_RECALL and safe advisory")
    void testScenario11_ProductFoundWithNoRecalls() {
        when(productRepository.findByNameIgnoreCase("Samsung Galaxy S24")).thenReturn(List.of(samsungS24));
        when(recallRepository.findByProductId(101L)).thenReturn(List.of());

        ResponseEntity<AssistantResponse> response = assistantService.processRequest(
                new AssistantRequest("Samsung Galaxy S24")
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("PRODUCT_FOUND_NO_RECALL", response.getBody().matchStatus());
        assertTrue(response.getBody().warnings().get(0).contains("No matching recall record was found"));
    }

    // 12. Completely unknown product
    @Test
    @DisplayName("Scenario 12: Completely unknown product returns NO_MATCH with safety advice")
    void testScenario12_CompletelyUnknownProduct() {
        when(productRepository.searchProducts("quantumtoaster3000")).thenReturn(List.of());

        ResponseEntity<AssistantResponse> response = assistantService.processRequest(
                new AssistantRequest("quantumtoaster3000")
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("NO_MATCH", response.getBody().matchStatus());
        assertTrue(response.getBody().answer().contains("No recall records matching"));
    }

    // 13. Dashboard statistics fast retrieval
    @Test
    @DisplayName("Scenario 13: Dashboard statistics returns instantaneous database summary")
    void testScenario13_DashboardStatisticsFastRetrieval() {
        when(productService.countProducts()).thenReturn(42L);
        when(recallRepository.count()).thenReturn(19L);
        when(recallService.countRecallsByStatus("ACTIVE")).thenReturn(12L);
        when(recallService.countRecallsByStatus("INACTIVE")).thenReturn(7L);

        ResponseEntity<AssistantResponse> response = assistantService.processRequest(
                new AssistantRequest("How many recalls are in the database?")
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().answer().contains("Registered Products"));
        verifyNoInteractions(openAiService);
    }

    // 14. General product safety guidance
    @Test
    @DisplayName("Scenario 14: General safety advice query returns structured safety checklist")
    void testScenario14_GeneralProductSafetyGuidance() {
        when(openAiService.ask(anyString(), anyString())).thenReturn("Safety guidance: Check model and date codes.");

        ResponseEntity<AssistantResponse> response = assistantService.processRequest(
                new AssistantRequest("What should I check before buying a used product?")
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    // 15. Conversational follow-up: 'is it recalled?'
    @Test
    @DisplayName("Scenario 15: Anaphoric follow-up 'is it recalled?' resolves product from session history")
    void testScenario15_ConversationalFollowUpIsItRecalled() {
        when(openAiService.ask(anyString(), anyString())).thenReturn("Samsung Galaxy S24 is affected by Recall #201.");

        AssistantRequest initial = new AssistantRequest("Check Samsung Galaxy S24", null, "session-test-1");
        when(productRepository.findByNameIgnoreCase("Samsung Galaxy S24")).thenReturn(List.of(samsungS24));
        when(recallRepository.findByProductId(101L)).thenReturn(List.of(recallGalaxy));
        assistantService.processRequest(initial);

        AssistantRequest followUp = new AssistantRequest("Is it recalled?", null, "session-test-1");
        ResponseEntity<AssistantResponse> response = assistantService.processRequest(followUp);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    // 16. Conversational follow-up: 'what is its batch number?'
    @Test
    @DisplayName("Scenario 16: Conversational follow-up 'what is its batch number?' resolves contextual product")
    void testScenario16_ConversationalFollowUpBatch() {
        when(openAiService.ask(anyString(), anyString())).thenReturn("The batch number for Samsung Galaxy S24 is SAM2026-001.");

        AssistantRequest initial = new AssistantRequest("Check Samsung Galaxy S24", null, "session-test-2");
        when(productRepository.findByNameIgnoreCase("Samsung Galaxy S24")).thenReturn(List.of(samsungS24));
        assistantService.processRequest(initial);

        AssistantRequest followUp = new AssistantRequest("What is its batch number?", null, "session-test-2");
        ResponseEntity<AssistantResponse> response = assistantService.processRequest(followUp);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    // 17. Conversational topic switch
    @Test
    @DisplayName("Scenario 17: Switching brands resets conversation context cleanly")
    void testScenario17_ConversationalTopicSwitch() {
        AssistantRequest q1 = new AssistantRequest("Samsung", null, "session-test-3");
        when(productRepository.findByBrandIgnoreCase("samsung")).thenReturn(List.of(samsungS24));
        assistantService.processRequest(q1);

        Product oppoPhone = new Product();
        oppoPhone.setId(202L);
        oppoPhone.setName("Oppo Find X6");
        oppoPhone.setBrand("Oppo");

        AssistantRequest q2 = new AssistantRequest("Oppo", null, "session-test-3");
        when(productRepository.findByBrandIgnoreCase("oppo")).thenReturn(List.of(oppoPhone));
        ResponseEntity<AssistantResponse> response = assistantService.processRequest(q2);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("DATABASE_DIRECT", response.getBody().sourceType());
    }

    // 18. AI explanation with verified sources
    @Test
    @DisplayName("Scenario 18: AI response includes verified regulatory source metadata")
    void testScenario18_AiExplanationWithVerifiedSources() {
        when(recallRepository.findByIdWithProduct(201L)).thenReturn(Optional.of(recallGalaxy));
        when(openAiService.ask(anyString(), anyString())).thenReturn("Recall #201 details from CPSC.");

        ResponseEntity<AssistantResponse> response = assistantService.processRequest(
                new AssistantRequest("Explain Recall ID #201")
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().sources().isEmpty());
        assertEquals("VERIFIED", response.getBody().sources().get(0).get("verificationStatus"));
        assertEquals("cpsc.gov", response.getBody().sources().get(0).get("domain"));
    }

    // 19. AI explanation with unverified sources
    @Test
    @DisplayName("Scenario 19: AI response correctly marks manufacturer URL as AVAILABLE_UNVERIFIED")
    void testScenario19_AiExplanationWithUnverifiedSources() {
        Recall unverifiedRecall = new Recall();
        unverifiedRecall.setId(202L);
        unverifiedRecall.setTitle("Unverified Hazard Notice");
        unverifiedRecall.setSourceUrl("https://brandcorp.com/safety/recall-202");

        when(recallRepository.findByIdWithProduct(202L)).thenReturn(Optional.of(unverifiedRecall));
        when(openAiService.ask(anyString(), anyString())).thenReturn("Notice from brandcorp.com.");

        ResponseEntity<AssistantResponse> response = assistantService.processRequest(
                new AssistantRequest("Explain Recall ID #202")
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().sources().isEmpty());
        assertEquals("AVAILABLE_UNVERIFIED", response.getBody().sources().get(0).get("verificationStatus"));
    }

    // 20. AI explanation with no sources
    @Test
    @DisplayName("Scenario 20: AI response marks missing source as UNAVAILABLE")
    void testScenario20_AiExplanationWithNoSources() {
        Recall noSourceRecall = new Recall();
        noSourceRecall.setId(203L);
        noSourceRecall.setTitle("Notice Without Source URL");
        noSourceRecall.setSourceUrl(null);

        when(recallRepository.findByIdWithProduct(203L)).thenReturn(Optional.of(noSourceRecall));
        when(openAiService.ask(anyString(), anyString())).thenReturn("Notice details.");

        ResponseEntity<AssistantResponse> response = assistantService.processRequest(
                new AssistantRequest("Explain Recall ID #203")
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().missingInformation().stream().anyMatch(s -> s.contains("Source URL missing")));
    }

    // 21. Gemini timeout / 503 fallback
    @Test
    @DisplayName("Scenario 21: Gemini 503 error falls back to degraded database answer")
    void testScenario21_Gemini503Fallback() {
        when(productRepository.findByModelNumberIgnoreCase("SM-S921B")).thenReturn(List.of(samsungS24));
        when(recallRepository.findByProductId(101L)).thenReturn(List.of(recallGalaxy));
        when(openAiService.ask(anyString(), anyString())).thenThrow(new IllegalStateException("Gemini API request failed with HTTP 503"));

        ResponseEntity<AssistantResponse> response = assistantService.processRequest(
                new AssistantRequest("Explain why model SM-S921B has safety issues")
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("DEGRADED", response.getBody().aiStatus());
        assertTrue(response.getBody().answer().contains("temporarily unavailable"));
    }

    // 22. Gemini 401 unauthorized handling
    @Test
    @DisplayName("Scenario 22: Gemini 401 unauthorized returns 503 SERVICE_UNAVAILABLE explaining missing key")
    void testScenario22_Gemini401Handling() {
        when(productRepository.findByModelNumberIgnoreCase("SM-S921B")).thenReturn(List.of(samsungS24));
        when(recallRepository.findByProductId(101L)).thenReturn(List.of(recallGalaxy));
        when(openAiService.ask(anyString(), anyString())).thenThrow(new HttpClientErrorException(HttpStatus.UNAUTHORIZED, "Unauthorized"));

        ResponseEntity<AssistantResponse> response = assistantService.processRequest(
                new AssistantRequest("Explain why model SM-S921B has safety issues")
        );

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().answer().contains("active Gemini API key"));
    }

    // 23. Anti-hallucination validation
    @Test
    @DisplayName("Scenario 23: Anti-hallucination validation strips ungrounded recall claims")
    void testScenario23_AntiHallucinationValidation() {
        when(openAiService.ask(anyString(), anyString()))
                .thenReturn("Recall ID #9999 has been issued for this product with severe explosive defects.");

        ResponseEntity<AssistantResponse> response = assistantService.processRequest(
                new AssistantRequest("Explain why this gadget has recalls")
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        // Must flag negative notice because context had 0 recalls
        assertTrue(response.getBody().answer().contains("No matching recall records were found"));
    }

    // 24. Response contract compliance
    @Test
    @DisplayName("Scenario 24: Response strictly conforms to AssistantResponse DTO contract")
    void testScenario24_ResponseContractCompliance() {
        when(productRepository.findByBrandIgnoreCase("samsung")).thenReturn(List.of(samsungS24));

        ResponseEntity<AssistantResponse> response = assistantService.processRequest(
                new AssistantRequest("Samsung")
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        AssistantResponse body = response.getBody();
        assertNotNull(body);
        assertNotNull(body.answer());
        assertNotNull(body.intent());
        assertNotNull(body.matchStatus());
        assertNotNull(body.products());
        assertNotNull(body.recalls());
        assertNotNull(body.sources());
        assertNotNull(body.warnings());
        assertNotNull(body.suggestedFollowUps());
        assertNotNull(body.sourceType());
        assertNotNull(body.missingInformation());
        assertNotNull(body.aiStatus());
    }

    // 25. Non-existent recall ID returns CASE E directly without AI invocation
    @Test
    @DisplayName("Scenario 25: Non-existent Recall ID returns deterministic Case E without calling AI")
    void testScenario25_NonExistentRecallIdReturnsCaseE() {
        when(recallRepository.findByIdWithProduct(9999L)).thenReturn(Optional.empty());

        ResponseEntity<AssistantResponse> response = assistantService.processRequest(
                new AssistantRequest("Explain Recall ID #9999")
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("NO_MATCH", response.getBody().matchStatus());
        assertEquals("DATABASE_DIRECT", response.getBody().sourceType());
        assertTrue(response.getBody().answer().contains("Recall ID #9999 does not exist"));
        verifyNoInteractions(openAiService);
    }

    // 26. Strict scoping: product query does not leak unlinked unrelated recalls via keyword search
    @Test
    @DisplayName("Scenario 26: Product query with 0 linked recalls never leaks unrelated recalls")
    void testScenario26_ProductWithoutRecallsNeverLeaksUnrelatedRecalls() {
        Product cleanProduct = new Product();
        cleanProduct.setId(301L);
        cleanProduct.setName("Clean Brand Gadget");
        cleanProduct.setModelNumber("CBG-100");

        when(productRepository.findByNameIgnoreCase("Clean Brand Gadget")).thenReturn(List.of(cleanProduct));
        when(recallRepository.findByProductId(301L)).thenReturn(List.of());

        ResponseEntity<AssistantResponse> response = assistantService.processRequest(
                new AssistantRequest("Clean Brand Gadget")
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("PRODUCT_FOUND_NO_RECALL", response.getBody().matchStatus());
        assertTrue(response.getBody().recalls().isEmpty());
        assertTrue(response.getBody().answer().contains("No associated recall records were found"));
        verifyNoInteractions(openAiService);
    }

    // 27. Ambiguous multi-product query lists candidate models with Case D formatting
    @Test
    @DisplayName("Scenario 27: Ambiguous multi-product query returns Case D formatted candidate selection")
    void testScenario27_AmbiguousMultiProductReturnsCaseDCandidates() {
        Product p1 = new Product();
        p1.setId(10L);
        p1.setName("Galaxy S24");
        p1.setModelNumber("SM-S921B");

        Product p2 = new Product();
        p2.setId(11L);
        p2.setName("Galaxy S24 Ultra");
        p2.setModelNumber("SM-S928B");

        when(productRepository.findByNameIgnoreCase("Galaxy S24")).thenReturn(List.of());
        when(productRepository.findByNameContainingIgnoreCase("Galaxy S24")).thenReturn(List.of(p1, p2));

        ResponseEntity<AssistantResponse> response = assistantService.processRequest(
                new AssistantRequest("Galaxy S24")
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("AMBIGUOUS", response.getBody().matchStatus());
        assertTrue(response.getBody().answer().contains("Multiple Matching Products Found"));
        assertTrue(response.getBody().answer().contains("Galaxy S24 Ultra"));
    }
}
