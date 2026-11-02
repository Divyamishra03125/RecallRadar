package com.recallradar.ai.service;

import com.recallradar.ai.*;
import com.recallradar.ai.dto.AssistantRequest;
import com.recallradar.ai.dto.AssistantResponse;
import com.recallradar.product.Product;
import com.recallradar.product.ProductRepository;
import com.recallradar.product.ProductService;
import com.recallradar.recall.Recall;
import com.recallradar.recall.RecallRepository;
import com.recallradar.recall.RecallService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class RecallAssistantService {

    private static final Logger log = LoggerFactory.getLogger(RecallAssistantService.class);

    private final ProductSearchService productSearchService;
    private final RecallRetrievalService recallRetrievalService;
    private final QueryIntentClassifier intentClassifier;
    private final ConversationContextService conversationContextService;
    private final RecallContextBuilder contextBuilder;
    private final AiResponseValidator responseValidator;
    private final OpenAiService openAiService;
    private final ProductRepository productRepository;
    private final ProductService productService;
    private final RecallRepository recallRepository;
    private final RecallService recallService;
    private final FollowUpGenerator followUpGenerator;

    @Autowired
    public RecallAssistantService(
            ProductSearchService productSearchService,
            RecallRetrievalService recallRetrievalService,
            QueryIntentClassifier intentClassifier,
            ConversationContextService conversationContextService,
            RecallContextBuilder contextBuilder,
            AiResponseValidator responseValidator,
            OpenAiService openAiService,
            ProductRepository productRepository,
            ProductService productService,
            RecallRepository recallRepository,
            RecallService recallService,
            FollowUpGenerator followUpGenerator
    ) {
        this.productSearchService = productSearchService;
        this.recallRetrievalService = recallRetrievalService;
        this.intentClassifier = intentClassifier != null ? intentClassifier : new QueryIntentClassifier();
        this.conversationContextService = conversationContextService != null ? conversationContextService : new ConversationContextService();
        this.contextBuilder = contextBuilder != null ? contextBuilder : new RecallContextBuilder();
        this.responseValidator = responseValidator != null ? responseValidator : new AiResponseValidator();
        this.openAiService = openAiService;
        this.productRepository = productRepository;
        this.productService = productService;
        this.recallRepository = recallRepository;
        this.recallService = recallService;
        this.followUpGenerator = followUpGenerator != null ? followUpGenerator : new FollowUpGenerator();
    }

    public Map<String, Object> checkHealth() {
        return openAiService != null ? openAiService.checkHealth() : Map.of("status", "DOWN", "message", "OpenAiService not initialized");
    }

    /**
     * Process an incoming user question, orchestrating multi-turn context resolution,
     * database retrieval, deterministic formatting, or Gemini explanation with anti-hallucination.
     */
    public ResponseEntity<AssistantResponse> processRequest(AssistantRequest request) {
        long startNs = System.nanoTime();

        if (request == null || !StringUtils.hasText(request.question())) {
            return ResponseEntity.badRequest()
                    .body(new AssistantResponse("Please enter a question."));
        }

        String rawQuestion = request.question().trim();
        List<ChatMessage> history = request.conversationHistory() != null
                ? request.conversationHistory()
                : List.of();
        String contextId = StringUtils.hasText(request.conversationContextId())
                ? request.conversationContextId().trim()
                : "default-session";

        // 1. Resolve effective question via conversational memory
        String question = conversationContextService.contextualizeQuery(rawQuestion, contextId, history);

        // 2. Classify user intent and extract entities
        QuestionClassification classification = intentClassifier.classify(question);
        long intentMs = (System.nanoTime() - startNs) / 1_000_000;

        log.info("Processing question: '{}' [Resolved: '{}', Intent: {}]", rawQuestion, question, classification.intent());

        // 3. Fast-path handling for informational / meta queries
        switch (classification.intent()) {
            case GREETING_OR_CHITCHAT -> {
                String ans = "Hello! I am RecallRadar AI, your intelligent product safety and risk analysis assistant. "
                        + "I can help you check if a product, brand, model number, or batch code has been recalled, "
                        + "explain safety hazards and remedies, provide real-time database statistics, or share general product safety guidance. "
                        + "How can I help you today?";
                List<String> followUps = followUpGenerator.generateFollowUps(classification, rawQuestion, List.of(), ans);
                long totalMs = (System.nanoTime() - startNs) / 1_000_000;
                log.info("Query '{}' -> Route: DIRECT_FAST_PATH [Total: {}ms | Intent: {}ms | DB: 0ms | AI: 0ms]", rawQuestion, totalMs, intentMs);
                return ResponseEntity.ok(new AssistantResponse(
                        ans, classification.intent().name(), "NO_MATCH",
                        List.of(), List.of(), List.of(), List.of(), followUps,
                        "STATIC_KNOWLEDGE", List.of(), "ONLINE", contextId
                ));
            }

            case HELP, APPLICATION_HELP, HELP_AND_CAPABILITIES -> {
                String ans = """
                        ### 🛡️ RecallRadar AI Capabilities

                        I am an intelligent Product Safety & Recall Analysis agent connected directly to your RecallRadar PostgreSQL database:

                        1. **Product Catalogue Search:** Look up registered products by name, brand, model number, or batch code.
                        2. **Recall Association & Verification:** Check whether a registered product is affected by active safety recalls.
                        3. **Recall Details & Traceability:** Inspect notice hazards, severity ratings, publication dates, and official source links.
                        4. **Risk & Severity Analysis:** Analyze risk distributions across CRITICAL, HIGH, MEDIUM, LOW, and UNKNOWN tiers.
                        5. **Live Platform Statistics:** Retrieve verified real-time counts of products, active enforcement notices, and inventory at risk.
                        6. **Product Safety Guidance:** General advice on checking labels, batch codes, or pre-purchase checklists.

                        *Try asking: "Check if Samsung Galaxy S24 has any recalls" or "How many products are registered in RecallRadar?"*
                        """;
                List<String> followUps = followUpGenerator.generateFollowUps(classification, rawQuestion, List.of(), ans);
                long totalMs = (System.nanoTime() - startNs) / 1_000_000;
                log.info("Query '{}' -> Route: DIRECT_FAST_PATH [Total: {}ms | Intent: {}ms | DB: 0ms | AI: 0ms]", rawQuestion, totalMs, intentMs);
                return ResponseEntity.ok(new AssistantResponse(
                        ans, classification.intent().name(), "NO_MATCH",
                        List.of(), List.of(), List.of(), List.of(), followUps,
                        "STATIC_KNOWLEDGE", List.of(), "ONLINE", contextId
                ));
            }

            case DASHBOARD_STATISTICS, DASHBOARD_ANALYSIS, DATABASE_STATISTICS -> {
                long totalProducts = productService != null ? productService.countProducts() : 0L;
                long totalRecalls = recallRepository != null ? recallRepository.count() : 0L;
                long activeCount = recallService.countRecallsByStatus("ACTIVE");
                long inactiveCount = recallService.countRecallsByStatus("INACTIVE");
                if (totalRecalls == 0 && (activeCount > 0 || inactiveCount > 0)) {
                    totalRecalls = activeCount + inactiveCount;
                }
                long criticalCount = countRecallsBySeverity("CRITICAL");
                long highCount = countRecallsBySeverity("HIGH");
                long unknownCount = countRecallsBySeverity("UNKNOWN");
                long productsAtRisk = calculateProductsAtRisk();

                String ans = String.format("""
                        ### 📊 RecallRadar Database Summary

                        | Platform Metric | Count | Description |
                        |---|---|---|
                        | 📦 **Registered Products** | **%d** | Total catalogue products registered |
                        | 📋 **Total Recalls Recorded** | **%d** | All recall notices recorded in database |
                        | 🟢 **Active Surveillance Recalls** | **%d** | Currently active safety enforcement notices |
                        | ⚪ **Inactive Recalls** | **%d** | Resolved or closed recall notices |
                        | ⚠️ **Distinct Products at Risk** | **%d** | Registered products with active safety notices |
                        | 🔴 **Critical Severity Tier** | **%d** | Highest hazard level notices |
                        | 🟠 **High Severity Tier** | **%d** | High hazard notices |
                        | ⚪ **Unknown Severity Tier** | **%d** | External notices without formal severity grade |

                        • Active Recalls: %d
                        • Inactive Recalls: %d
                        • Total Recalls Recorded: %d

                        *Summary grounded in active RecallRadar PostgreSQL records.*
                        """, totalProducts, totalRecalls, activeCount, inactiveCount, productsAtRisk, criticalCount, highCount, unknownCount,
                        activeCount, inactiveCount, totalRecalls);

                List<String> followUps = followUpGenerator.generateFollowUps(classification, rawQuestion, List.of(), ans);
                long totalMs = (System.nanoTime() - startNs) / 1_000_000;
                log.info("Query '{}' -> Route: DIRECT_FAST_PATH [Total: {}ms | Intent: {}ms | DB: {}ms | AI: 0ms]", rawQuestion, totalMs, intentMs, totalMs - intentMs);
                return ResponseEntity.ok(new AssistantResponse(
                        ans, classification.intent().name(), "NO_MATCH",
                        List.of(), List.of(), List.of(), List.of(), followUps,
                        "DATABASE_DIRECT", List.of(), "ONLINE", contextId
                ));
            }

            case REPORT_ASSISTANCE -> {
                String ans = """
                        ### 📑 RecallRadar Reporting & Export Capabilities

                        RecallRadar provides automated audit reports and compliance exports directly from your registered safety database:

                        1. **Executive Safety Summary (PDF):** High-level summary of total recalls, active surveillance monitors, and risk severity distribution.
                        2. **Detailed Recall Inventory (CSV):** Complete spreadsheet export including product identifiers, model codes, batch numbers, hazard descriptions, and official source URLs.
                        3. **Risk Audit Brief:** Targeted breakdown by brand, category, or critical severity level.

                        *Navigate to the **Reports** page in the sidebar to download PDF or CSV files, or ask me to summarize specific metrics!*
                        """;
                List<String> followUps = followUpGenerator.generateFollowUps(classification, rawQuestion, List.of(), ans);
                long totalMs = (System.nanoTime() - startNs) / 1_000_000;
                log.info("Query '{}' -> Route: DIRECT_FAST_PATH [Total: {}ms | Intent: {}ms | DB: 0ms | AI: 0ms]", rawQuestion, totalMs, intentMs);
                return ResponseEntity.ok(new AssistantResponse(
                        ans, classification.intent().name(), "NO_MATCH",
                        List.of(), List.of(), List.of(), List.of(), followUps,
                        "STATIC_KNOWLEDGE", List.of(), "ONLINE", contextId
                ));
            }

            default -> {}
        }

        // 4. RETRIEVE PRODUCTS & RECALLS FROM POSTGRESQL
        long dbStartNs = System.nanoTime();
        List<Product> matchingProducts = productSearchService.searchProducts(
                question,
                classification.productId(),
                classification.brand(),
                classification.modelNumber(),
                classification.batchNumber()
        );

        List<Recall> relevantRecalls = recallRetrievalService.searchRecalls(
                question,
                classification.recallId(),
                classification.brand(),
                classification.modelNumber(),
                classification.batchNumber(),
                classification.severity(),
                classification.targetStatus(),
                classification.dateFrom(),
                classification.dateTo(),
                matchingProducts
        );
        long dbMs = (System.nanoTime() - dbStartNs) / 1_000_000;

        // Check explicit Recall ID not found (CASE E)
        if (classification.recallId() != null && relevantRecalls.isEmpty()) {
            String ans = RecallResponseFormatter.formatCaseERecallNotFound(classification.recallId());
            List<String> followUps = followUpGenerator.generateFollowUps(classification, rawQuestion, List.of(), ans);
            List<String> warnings = List.of("Recall ID #" + classification.recallId() + " does not exist in the current RecallRadar database.");
            long totalMs = (System.nanoTime() - startNs) / 1_000_000;
            log.info("Query '{}' -> Route: DIRECT_DATABASE_PATH (CASE E: Recall ID Not Found) [Total: {}ms | Intent: {}ms | DB: {}ms | AI: 0ms]",
                    rawQuestion, totalMs, intentMs, dbMs);
            return ResponseEntity.ok(new AssistantResponse(
                    ans,
                    classification.intent().name(),
                    "NO_MATCH",
                    List.of(), List.of(), List.of(), warnings, followUps,
                    "DATABASE_DIRECT", List.of(), "ONLINE", contextId
            ));
        }

        // 5. CONSTRUCT STRUCTURED ENTITIES
        List<Map<String, Object>> productSummaries = buildProductSummaries(matchingProducts);
        List<Map<String, Object>> recallSummaries = buildRecallSummaries(relevantRecalls);
        List<Map<String, Object>> sourceSummaries = buildSourceSummaries(relevantRecalls);

        String matchStatus;
        if (matchingProducts.size() > 1) {
            matchStatus = "AMBIGUOUS";
        } else if (!relevantRecalls.isEmpty()) {
            matchStatus = "MATCH_FOUND";
        } else if (!matchingProducts.isEmpty()) {
            matchStatus = "PRODUCT_FOUND_NO_RECALL";
        } else {
            matchStatus = "NO_MATCH";
        }

        List<String> warnings = new ArrayList<>();
        if (relevantRecalls.isEmpty()) {
            warnings.add("No matching recall record was found in the current RecallRadar database. This does not guarantee that the product has never been recalled or is completely safe. Check the manufacturer and relevant official product-safety authorities for the latest information.");
        }
        if ("AMBIGUOUS".equals(matchStatus)) {
            warnings.add("Multiple matching products were found in the database. Please specify the exact model name or batch identifier for more precise recall analysis.");
        }

        // 6. UPDATE CONVERSATIONAL STATE
        conversationContextService.updateState(contextId, classification.intent(), matchingProducts, relevantRecalls, classification);

        // 7. CHECK FOR DIRECT DATABASE PATH (Fast Database-First Retrieval)
        if (isDirectDatabasePath(rawQuestion, question, classification)) {
            String ans;
            if ("AMBIGUOUS".equals(matchStatus)) {
                ans = RecallResponseFormatter.formatCaseDAmbiguousProducts(rawQuestion, matchingProducts);
            } else if ((classification.intent() == QuestionIntent.BATCH_SEARCH || classification.intent() == QuestionIntent.BATCH_INVESTIGATION || classification.intent() == QuestionIntent.BATCH_NUMBER_SEARCH)
                    && classification.batchNumber() != null) {
                ans = RecallResponseFormatter.formatDirectBatchResponse(classification.batchNumber(), matchingProducts, relevantRecalls);
            } else if (!matchingProducts.isEmpty()) {
                ans = RecallResponseFormatter.formatDirectProductSearchResponse(rawQuestion, matchingProducts, relevantRecalls);
            } else if (classification.intent() == QuestionIntent.RECALL_SEARCH
                    || classification.intent() == QuestionIntent.ACTIVE_RECALL_SEARCH
                    || classification.intent() == QuestionIntent.SEVERITY_SEARCH
                    || classification.intent() == QuestionIntent.SEVERITY_ANALYSIS
                    || classification.intent() == QuestionIntent.STATUS_SEARCH) {
                ans = RecallResponseFormatter.formatDirectRecallSearchResponse(rawQuestion, relevantRecalls);
            } else {
                ans = RecallResponseFormatter.formatDirectProductSearchResponse(rawQuestion, matchingProducts, relevantRecalls);
            }

            List<String> followUps = followUpGenerator.generateFollowUps(classification, rawQuestion, relevantRecalls, ans);
            long totalMs = (System.nanoTime() - startNs) / 1_000_000;
            log.info("Query '{}' -> Route: DIRECT_DATABASE_PATH [Total: {}ms | Intent: {}ms | DB: {}ms | AI: 0ms]",
                    rawQuestion, totalMs, intentMs, dbMs);

            return ResponseEntity.ok(new AssistantResponse(
                    ans,
                    classification.intent().name(),
                    matchStatus,
                    productSummaries,
                    recallSummaries,
                    sourceSummaries,
                    warnings,
                    followUps,
                    "DATABASE_DIRECT",
                    extractMissingInformation(relevantRecalls),
                    "ONLINE",
                    contextId
            ));
        }

        // 8. BUILD GROUNDED CONTEXT
        long activeRecalls = recallService != null ? recallService.countRecallsByStatus("ACTIVE") : 0L;
        String context = contextBuilder.buildStructuredContext(question, classification, matchingProducts, relevantRecalls, activeRecalls);

        // 9. CALL GEMINI FOR NATURAL-LANGUAGE EXPLANATION
        long aiStartNs = System.nanoTime();
        String answer;
        try {
            answer = openAiService.ask(question, context);
            long aiMs = (System.nanoTime() - aiStartNs) / 1_000_000;
            long totalMs = (System.nanoTime() - startNs) / 1_000_000;
            log.info("Query '{}' -> Route: GEMINI_AI_PATH [Total: {}ms | Intent: {}ms | DB: {}ms | AI: {}ms]",
                    rawQuestion, totalMs, intentMs, dbMs, aiMs);

            AiResponseValidator.ValidationResult valResult = responseValidator.validate(answer, context, !relevantRecalls.isEmpty(), classification.intent());
            answer = valResult.validatedAnswer();
            warnings.addAll(valResult.warnings());

            List<String> followUps = followUpGenerator.generateFollowUps(classification, rawQuestion, relevantRecalls, answer);
            return ResponseEntity.ok(new AssistantResponse(
                    answer,
                    classification.intent().name(),
                    matchStatus,
                    productSummaries,
                    recallSummaries,
                    sourceSummaries,
                    warnings,
                    followUps,
                    "GEMINI_GROUNDED",
                    extractMissingInformation(relevantRecalls),
                    "ONLINE",
                    contextId
            ));

        } catch (Exception ex) {
            long aiMs = (System.nanoTime() - aiStartNs) / 1_000_000;
            long totalMs = (System.nanoTime() - startNs) / 1_000_000;
            log.warn("Gemini AI request failed: {} [AI elapsed: {}ms | Total: {}ms]. Falling back to deterministic database answer.",
                    ex.getMessage(), aiMs, totalMs);

            if (ex instanceof HttpClientErrorException httpEx && httpEx.getStatusCode().value() == 401
                    || (ex.getMessage() != null && ex.getMessage().contains("401"))) {
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                        .body(new AssistantResponse("The RecallRadar AI assistant requires a valid, active Gemini API key to generate natural-language answers."));
            }

            // General safety guidance fallback
            if (classification.intent() == QuestionIntent.GENERAL_PRODUCT_SAFETY || classification.intent() == QuestionIntent.GENERAL_SAFETY) {
                String ans = getKnowledgeBaseSafetyGuidance();
                List<String> followUps = followUpGenerator.generateFollowUps(classification, rawQuestion, List.of(), ans);
                return ResponseEntity.ok(new AssistantResponse(
                        ans, classification.intent().name(), "NO_MATCH",
                        List.of(), List.of(), List.of(), List.of(), followUps,
                        "STATIC_KNOWLEDGE", List.of(), "DEGRADED", contextId
                ));
            }

            // Recall records database fallback
            if (!relevantRecalls.isEmpty()) {
                String ans = RecallResponseFormatter.formatDegradedResponse(relevantRecalls);
                List<String> followUps = followUpGenerator.generateFollowUps(classification, rawQuestion, relevantRecalls, ans);
                return ResponseEntity.ok(new AssistantResponse(
                        ans, classification.intent().name(), "MATCH_FOUND",
                        productSummaries, recallSummaries, sourceSummaries, warnings, followUps,
                        "DEGRADED_DATABASE", extractMissingInformation(relevantRecalls), "DEGRADED", contextId
                ));
            }

            // Product found without recall records fallback
            if (!matchingProducts.isEmpty()) {
                StringBuilder sb = new StringBuilder();
                sb.append("I found the following registered product in the RecallRadar catalogue:\n\n");
                for (Product p : matchingProducts) {
                    sb.append(RecallResponseFormatter.formatProductCard(p)).append("\n\n");
                }
                sb.append("However, no associated recall records were found in the current RecallRadar database.\n")
                  .append("Absence of a recall record does not guarantee that a product has never been recalled. Always verify with official safety portals.");
                String ans = sb.toString();
                List<String> followUps = followUpGenerator.generateFollowUps(classification, rawQuestion, List.of(), ans);
                return ResponseEntity.ok(new AssistantResponse(
                        ans, classification.intent().name(), matchStatus,
                        productSummaries, List.of(), List.of(), warnings, followUps,
                        "DEGRADED_DATABASE", List.of(), "DEGRADED", contextId
                ));
            }

            // Zero matches graceful notice
            StringBuilder sb = new StringBuilder();
            sb.append("### 🔍 RecallRadar Database Search Result\n\n");
            sb.append("No matching product or recall records were found in the RecallRadar database for **\"")
              .append(question).append("\"**.\n\n");
            sb.append("---\n\n");
            sb.append("### 🛡️ Product Safety Advice\n\n");
            sb.append("- The absence of a recall record in the RecallRadar database does not guarantee that a product is completely safe, certified safe, or hazard-free.\n");
            sb.append("- We recommend verifying directly with the manufacturer or checking official regulatory safety databases such as [CPSC.gov](https://www.cpsc.gov) or [SaferProducts.gov](https://www.saferproducts.gov).\n\n");
            sb.append("⚠️ *Safety Advisory: Absence of a recall record in the RecallRadar database does not guarantee that a product is completely safe or free from defects. Always verify with the manufacturer or official safety portals such as SaferProducts.gov or CPSC.gov.*");
            String ans = sb.toString();
            List<String> followUps = followUpGenerator.generateFollowUps(classification, rawQuestion, List.of(), ans);
            return ResponseEntity.ok(new AssistantResponse(
                    ans, classification.intent().name(), "NO_MATCH",
                    List.of(), List.of(), List.of(), warnings, followUps,
                    "DEGRADED_DATABASE", List.of(), "DEGRADED", contextId
            ));
        }
    }

    private boolean isReasoningOrExplanationQuery(String rawQuestion, String effectiveQuestion, QuestionClassification classification) {
        String lower = (rawQuestion + " " + effectiveQuestion).toLowerCase(Locale.ROOT);

        if (lower.contains("explain")
                || lower.contains("why ") || lower.endsWith("why")
                || lower.contains("summarize") || lower.contains("narrative")
                || lower.contains("compare") || lower.contains(" versus ") || lower.contains(" vs ")
                || lower.contains("how dangerous") || lower.contains("what should i do")
                || lower.contains("what action") || lower.contains("recommendation")
                || lower.contains("remedy") || lower.contains("remedies")
                || lower.contains("deep dive") || lower.contains("elaborate")
                || lower.contains("consequence") || lower.contains("symptom")
                || lower.contains("advice") || lower.contains("guidance")) {
            return true;
        }

        if (classification.intent() == QuestionIntent.PRODUCT_COMPARISON
                || classification.intent() == QuestionIntent.RISK_ANALYSIS
                || classification.intent() == QuestionIntent.GENERAL_SAFETY
                || classification.intent() == QuestionIntent.GENERAL_PRODUCT_SAFETY) {
            return true;
        }

        return false;
    }

    public boolean isDirectDatabasePath(String rawQuestion, String question, QuestionClassification classification) {
        if (isReasoningOrExplanationQuery(rawQuestion, question, classification)) {
            return false;
        }

        QuestionIntent intent = classification.intent();

        // 1. Direct product and catalogue searches (e.g., Redmi, Oppo, Samsung, Laptop, Find model SM-S921B)
        if (intent == QuestionIntent.PRODUCT_SEARCH || intent == QuestionIntent.PRODUCT_DETAILS
                || intent == QuestionIntent.MODEL_SEARCH || intent == QuestionIntent.MODEL_NUMBER_SEARCH) {
            return true;
        }

        // 2. Recall searches and severity queries (e.g., Show active recalls, active recalls, Show critical recalls)
        if (intent == QuestionIntent.RECALL_SEARCH || intent == QuestionIntent.ACTIVE_RECALL_SEARCH
                || intent == QuestionIntent.SEVERITY_ANALYSIS || intent == QuestionIntent.SEVERITY_SEARCH
                || intent == QuestionIntent.STATUS_SEARCH || intent == QuestionIntent.RECALL_ID_SEARCH) {
            return true;
        }

        // 3. Batch searches (e.g. "Check batch SAM2026-001", "batch SAM2026-001")
        if (intent == QuestionIntent.BATCH_INVESTIGATION || intent == QuestionIntent.BATCH_SEARCH
                || intent == QuestionIntent.BATCH_NUMBER_SEARCH) {
            String lower = rawQuestion.toLowerCase(Locale.ROOT).trim();
            if (!lower.contains("for recalls") && !lower.contains("model")) {
                return true;
            }
        }

        return false;
    }

    private List<String> extractMissingInformation(List<Recall> recalls) {
        if (recalls == null || recalls.isEmpty()) return List.of();
        List<String> missing = new ArrayList<>();
        for (Recall r : recalls) {
            if (r.getRecallDate() == null) missing.add("Publication date missing on Recall #" + r.getId());
            if (!StringUtils.hasText(r.getSourceUrl())) missing.add("Source URL missing on Recall #" + r.getId());
            if (r.getProduct() == null) missing.add("Catalogue product unlinked on Recall #" + r.getId());
            if ("UNKNOWN".equalsIgnoreCase(r.getSeverity())) missing.add("Severity grade UNKNOWN on Recall #" + r.getId());
        }
        return missing;
    }

    private long countRecallsBySeverity(String severity) {
        List<Object[]> rows = recallRepository.countRecallsBySeverity();
        for (Object[] row : rows) {
            if (row.length >= 2 && severity.equalsIgnoreCase(String.valueOf(row[0]))) {
                return ((Number) row[1]).longValue();
            }
        }
        return 0L;
    }

    private long calculateProductsAtRisk() {
        return recallRepository.findAll().stream()
                .filter(r -> "ACTIVE".equalsIgnoreCase(r.getStatus()))
                .map(Recall::getProduct)
                .filter(Objects::nonNull)
                .map(Product::getId)
                .distinct()
                .count();
    }

    private String getKnowledgeBaseSafetyGuidance() {
        return """
                ### 🛡️ Product Safety & Recall Checklist

                When evaluating whether a product may be subject to a safety notice:

                1. **Model & Batch Number Verification:** Look for the model number, serial number, and manufacture/batch date code (often located on the back, underside, or battery compartment).
                2. **Search Regulatory Portals:** Cross-reference official databases such as [CPSC.gov](https://www.cpsc.gov) (US Consumer Product Safety Commission), [FDA.gov](https://www.fda.gov), and [SaferProducts.gov](https://www.saferproducts.gov).
                3. **Inspect Active Safety Notices:** If a recall exists, do not attempt to repair the product yourself. Follow manufacturer instructions for remedies (free repair, replacement, or refund).
                4. **Register Your Products:** Registering high-risk household items (electronics, infant care, vehicles) ensures you receive direct notification in the event of an enforcement action.

                ⚠️ *Safety Advisory: Absence of a recall record in the RecallRadar database does not guarantee that a product is completely safe or free from defects. Always verify with the manufacturer or official safety portals such as SaferProducts.gov or CPSC.gov.*
                """;
    }

    private List<Map<String, Object>> buildProductSummaries(List<Product> products) {
        if (products == null) return List.of();
        return products.stream()
                .limit(10)
                .map(p -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("id", p.getId());
                    map.put("name", p.getName());
                    map.put("brand", p.getBrand());
                    map.put("category", p.getCategory());
                    map.put("modelNumber", p.getModelNumber());
                    map.put("batchNumber", p.getBatchNumber());
                    return map;
                })
                .collect(Collectors.toList());
    }

    private List<Map<String, Object>> buildRecallSummaries(List<Recall> recalls) {
        if (recalls == null) return List.of();
        return recalls.stream()
                .limit(10)
                .map(r -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("id", r.getId());
                    map.put("sourceRecordId", r.getSourceRecordId());
                    map.put("title", r.getTitle());
                    map.put("severity", r.getSeverity());
                    map.put("severityEvidence", r.getSeverityEvidence());
                    map.put("severityClassificationSource", r.getSeverityClassificationSource());
                    map.put("status", r.getStatus());
                    map.put("associationStatus", r.getAssociationStatus());
                    map.put("recallDate", r.getRecallDate() != null ? r.getRecallDate().toString() : null);
                    map.put("sourceUrl", r.getSourceUrl());
                    map.put("verificationStatus", RecallResponseFormatter.getSourceVerificationStatus(r.getSourceUrl()));
                    if (r.getProduct() != null) {
                        map.put("productId", r.getProduct().getId());
                        map.put("productName", r.getProduct().getName());
                        map.put("brand", r.getProduct().getBrand());
                        map.put("modelNumber", r.getProduct().getModelNumber());
                        map.put("batchNumber", r.getProduct().getBatchNumber());
                    }
                    return map;
                })
                .collect(Collectors.toList());
    }

    private List<Map<String, Object>> buildSourceSummaries(List<Recall> recalls) {
        if (recalls == null) return List.of();
        Map<String, Map<String, Object>> uniqueSources = new LinkedHashMap<>();

        for (Recall r : recalls) {
            String url = r.getSourceUrl();
            if (StringUtils.hasText(url) && !uniqueSources.containsKey(url)) {
                String domain = RecallResponseFormatter.extractDomain(url);
                String verificationStatus = RecallResponseFormatter.getSourceVerificationStatus(url);
                Map<String, Object> src = new LinkedHashMap<>();
                src.put("title", r.getTitle());
                src.put("sourceUrl", url);
                src.put("domain", domain);
                src.put("verificationStatus", verificationStatus);
                src.put("recallId", r.getId());
                uniqueSources.put(url, src);
            }
        }

        return new ArrayList<>(uniqueSources.values());
    }
}
