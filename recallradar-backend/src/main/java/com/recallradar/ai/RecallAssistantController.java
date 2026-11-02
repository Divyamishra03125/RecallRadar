package com.recallradar.ai;

import com.recallradar.ai.dto.AssistantRequest;
import com.recallradar.ai.dto.AssistantResponse;
import com.recallradar.ai.service.*;
import com.recallradar.product.ProductRepository;
import com.recallradar.product.ProductService;
import com.recallradar.recall.RecallRepository;
import com.recallradar.recall.RecallService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/ai")
@CrossOrigin(
        origins = {
                "http://localhost:5173",
                "http://localhost:5174",
                "http://localhost:3000",
                "http://127.0.0.1:5173",
                "http://127.0.0.1:5174"
        },
        allowCredentials = "true"
)
public class RecallAssistantController {

    private static final Logger log = LoggerFactory.getLogger(RecallAssistantController.class);

    private final RecallAssistantService assistantService;

    @Autowired
    public RecallAssistantController(RecallAssistantService assistantService) {
        this.assistantService = assistantService;
    }

    /**
     * Backward-compatible constructor supporting existing unit tests with mocked dependencies.
     */
    public RecallAssistantController(
            OpenAiService openAiService,
            RecallRepository recallRepository,
            RecallService recallService,
            ProductRepository productRepository,
            ProductService productService,
            QuestionClassifier questionClassifier,
            FollowUpGenerator followUpGenerator
    ) {
        this(new RecallAssistantService(
                new ProductSearchService(productRepository, recallRepository),
                new RecallRetrievalService(recallRepository),
                new QueryIntentClassifier(questionClassifier),
                new ConversationContextService(),
                new RecallContextBuilder(),
                new AiResponseValidator(),
                openAiService,
                productRepository,
                productService,
                recallRepository,
                recallService,
                followUpGenerator
        ));
    }

    public RecallAssistantController(
            OpenAiService openAiService,
            RecallRepository recallRepository,
            RecallService recallService,
            ProductRepository productRepository,
            ProductService productService
    ) {
        this(openAiService, recallRepository, recallService, productRepository, productService,
                new QuestionClassifier(), new FollowUpGenerator());
    }

    // ==========================================
    // AI ASSISTANT API ENDPOINT
    // ==========================================

    @PostMapping("/assistant")
    public ResponseEntity<AssistantResponse> askAssistant(@RequestBody AssistantRequest request) {
        return assistantService.processRequest(request);
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> checkAiHealth() {
        Map<String, Object> health = assistantService.checkHealth();
        org.springframework.http.HttpStatus status = "DOWN".equalsIgnoreCase((String) health.get("status"))
                ? org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE
                : org.springframework.http.HttpStatus.OK;
        return ResponseEntity.status(status).body(health);
    }
}
