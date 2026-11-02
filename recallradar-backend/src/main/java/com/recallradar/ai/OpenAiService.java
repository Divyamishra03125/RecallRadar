package com.recallradar.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class OpenAiService {

    private static final Logger log = LoggerFactory.getLogger(OpenAiService.class);

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String primaryModel;
    private final String fallbackModel;
    private final int maxRetries;
    private final long retryDelayMs;

    private volatile Map<String, Object> cachedHealth;
    private volatile long lastHealthCheckTime = 0L;
    private static final long HEALTH_CACHE_TTL_MS = 30_000L;

    @Autowired
    public OpenAiService(
            @Value("${gemini.api-key:}") String apiKey,
            @Value("${gemini.model:gemini-3.5-flash}") String model,
            @Value("${gemini.fallback-model:gemini-flash-lite-latest}") String fallbackModel,
            @Value("${gemini.max-retries:3}") int maxRetries,
            @Value("${gemini.retry-delay-ms:1000}") long retryDelayMs) {

        this(
                createDefaultRestClient(),
                apiKey,
                model,
                fallbackModel,
                maxRetries,
                retryDelayMs,
                new ObjectMapper()
        );
    }

    private static RestClient createDefaultRestClient() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(3));
        requestFactory.setReadTimeout(Duration.ofSeconds(6));
        return RestClient.builder()
                .requestFactory(requestFactory)
                .baseUrl("https://generativelanguage.googleapis.com")
                .build();
    }

    public OpenAiService(
            RestClient restClient,
            String apiKey,
            String model,
            String fallbackModel,
            int maxRetries,
            long retryDelayMs,
            ObjectMapper objectMapper) {

        this.restClient = restClient;
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.primaryModel = StringUtils.hasText(model) ? model.trim() : "gemini-3.5-flash";
        this.fallbackModel = StringUtils.hasText(fallbackModel) ? fallbackModel.trim() : "gemini-flash-lite-latest";
        this.maxRetries = Math.max(1, maxRetries);
        this.retryDelayMs = Math.max(10, retryDelayMs);
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
    }

    public String getPrimaryModel() {
        return primaryModel;
    }

    public String getFallbackModel() {
        return fallbackModel;
    }

    public boolean isApiKeyConfigured() {
        return !apiKey.isBlank();
    }

    // ==========================================
    // GEMINI AI REQUEST WITH RESILIENCE & RETRIES
    // ==========================================

    public String ask(String question, String recallContext) {

        if (apiKey.isBlank()) {
            throw new IllegalStateException(
                    "Gemini API key is missing. "
                            + "Set RECALLRADAR_GEMINI_API_KEY "
                            + "in the environment and restart the backend."
            );
        }

        // ==========================================
        // SYSTEM INSTRUCTIONS (ANTI-HALLUCINATION)
        // ==========================================

        String instructions = """
                You are RecallRadar, an intelligent product recall
                analysis assistant.

                Your responsibility is to help users understand
                product recalls using accurate database information.

                STRICT DATA ACCURACY & ANTI-HALLUCINATION RULES:

                1. Use the supplied RecallRadar database context as the absolute source of truth for all product and recall records.
                2. Ground every recall statement strictly in the provided database context. Only cite Recall IDs that explicitly appear in the context.
                3. NEVER invent or hallucinate recall IDs, product names, brands, model numbers, batch numbers, dates, statuses, severity levels, or hazards.
                4. If the database context indicates the product was not found in the catalogue, state clearly: "The product is not registered in the RecallRadar catalogue." Do NOT say "no recalls found".
                5. If the product is found in the catalogue but has 0 associated recalls, state: "No recall notices are currently associated with this product in the RecallRadar database." Always include the disclaimer that this does not prove the product is safe or certified defect-free.
                6. If multiple distinct products match, list the matching candidate products and ask the user to specify their model number or batch code.
                7. If a queried Recall ID does not exist in the database, explicitly state: "Recall ID #[id] does not exist in the current database."
                8. Differentiate severity tiers clearly: 🔴 CRITICAL, 🟠 HIGH, 🟡 MEDIUM, 🔵 LOW, and ⚪ UNKNOWN. If severity is missing, state "⚪ UNKNOWN".
                9. If recall date is missing, display "Date not available". Never infer or guess dates from URLs or names.
                10. If source URL is missing or unavailable, state "Source unavailable". Never generate fake or hallucinated URLs.
                11. Preserve the exact recall status (ACTIVE / INACTIVE) supplied by the database.
                12. Clearly distinguish verified database records from general safety guidance.
                13. Always encourage users to verify critical safety information with the manufacturer or official regulatory safety authorities (e.g. CPSC, FDA, NHTSA).

                RESPONSE FORMAT:

                Use clear, professional and understandable language formatted in clean Markdown.

                When matching recall records exist, format each recall as a structured Markdown card:
                ### ⚠️ Recall ID #[id]: [Title]
                - **Product:** [Product name]
                - **Brand:** [Brand]
                - **Category:** [Category]
                - **Model Number:** `[model number]`
                - **Batch / Lot Number:** `[batch number]`
                - **Recall Status:** 🟢 ACTIVE or ⚪ INACTIVE (match status from context)
                - **Severity Level:** 🔴 CRITICAL / 🟠 HIGH / 🟡 MEDIUM / 🔵 LOW / ⚪ UNKNOWN
                - **Recall Date:** [Date or "Date not available"]

                **Hazard & Risk:**
                [Summarize the hazard clearly and concisely]

                **Official Notice:**
                [View Official Recall Notice](url) (or "Source unavailable" if missing)

                When presenting multiple recalls, separate them with a horizontal rule (`---`).
                Do not fabricate missing fields.

                If no records match, state:
                "No matching recall records were found in the RecallRadar database."
                and provide safety checking advice.

                For general product safety questions, provide
                practical guidance and distinguish it from
                verified database information.

                Always encourage users to verify important
                safety information using the manufacturer
                or relevant official government source.
                """;

        String safeContext = recallContext == null
                ? "No database context was supplied."
                : recallContext;

        String safeQuestion = question == null
                ? ""
                : question.trim();

        String input = """
                RECALLRADAR DATABASE CONTEXT:

                %s

                USER QUESTION:

                %s

                Answer using the supplied database information
                and follow all system accuracy instructions.
                """.formatted(safeContext, safeQuestion);

        Map<String, Object> requestBody = Map.of(
                "systemInstruction",
                Map.of(
                        "parts",
                        List.of(
                                Map.of("text", instructions)
                        )
                ),
                "contents",
                List.of(
                        Map.of(
                                "role", "user",
                                "parts",
                                List.of(
                                        Map.of("text", input)
                                )
                        )
                ),
                "generationConfig",
                Map.of(
                        "temperature", 0.2,
                        "maxOutputTokens", 800
                )
        );

        String rawAnswer = executeWithResilience(requestBody);
        return validateAndEnforceGrounding(rawAnswer, safeContext);
    }

    // ==========================================
    // EXECUTION RESILIENCE (RETRY & FAILOVER)
    // ==========================================

    private String executeWithResilience(Map<String, Object> requestBody) {

        List<String> modelsToTry = new ArrayList<>();
        modelsToTry.add(primaryModel);
        if (!modelsToTry.contains(fallbackModel)) {
            modelsToTry.add(fallbackModel);
        }
        if (!modelsToTry.contains("gemini-3.5-flash")) {
            modelsToTry.add("gemini-3.5-flash");
        }
        if (!modelsToTry.contains("gemini-flash-lite-latest")) {
            modelsToTry.add("gemini-flash-lite-latest");
        }
        if (!modelsToTry.contains("gemini-3.1-flash-lite")) {
            modelsToTry.add("gemini-3.1-flash-lite");
        }
        if (!modelsToTry.contains("gemini-3.5-flash-lite")) {
            modelsToTry.add("gemini-3.5-flash-lite");
        }
        if (!modelsToTry.contains("gemini-3.6-flash")) {
            modelsToTry.add("gemini-3.6-flash");
        }

        Exception lastException = null;

        for (int modelIndex = 0; modelIndex < modelsToTry.size(); modelIndex++) {

            String activeModel = modelsToTry.get(modelIndex);
            boolean isFallback = modelIndex > 0;

            if (isFallback) {
                log.warn("Failing over to fallback Gemini model: {}", activeModel);
            }

            for (int attempt = 1; attempt <= maxRetries; attempt++) {

                try {
                    log.debug(
                            "Calling Gemini API (model: {}, attempt: {}/{})",
                            activeModel,
                            attempt,
                            maxRetries
                    );

                    return executeGeminiRequest(activeModel, requestBody);

                } catch (RestClientResponseException ex) {

                    int statusCode = ex.getStatusCode().value();
                    lastException = ex;

                    log.error(
                            "Gemini API returned HTTP {} on attempt {}/{} for model {}: {}",
                            statusCode,
                            attempt,
                            maxRetries,
                            activeModel,
                            ex.getResponseBodyAsString()
                    );

                    // Authentication / authorization failure is not retryable
                    if (statusCode == 401 || statusCode == 403) {
                        throw new IllegalStateException(
                                "Gemini API key is invalid or unauthorized (HTTP "
                                        + statusCode + "). Please check your API key configuration.",
                                ex
                        );
                    }

                    // Model not found or deprecated: stop retrying this model and switch to fallback
                    if (statusCode == 404 || (statusCode == 400 && ex.getResponseBodyAsString().contains("models/"))) {
                        log.warn(
                                "Model '{}' was not found or is unavailable (HTTP {}). Skipping further retries on this model.",
                                activeModel,
                                statusCode
                        );
                        break;
                    }

                    // Daily quota exhaustion: if daily free-tier limit is exhausted, do NOT retry on same model; fail over immediately
                    if (statusCode == 429 && ex.getResponseBodyAsString() != null
                            && (ex.getResponseBodyAsString().contains("RESOURCE_EXHAUSTED")
                            || ex.getResponseBodyAsString().contains("quotaValue"))) {
                        log.warn(
                                "Model '{}' daily quota is exhausted (RESOURCE_EXHAUSTED). Skipping retries and failing over immediately.",
                                activeModel
                        );
                        break;
                    }

                    // Transient errors: 503 (Overloaded / Unavailable), 429 (Rate limit), 500, 502, 504
                    if (isTransientStatus(statusCode)) {
                        if (attempt < maxRetries) {
                            long backoff = calculateBackoff(attempt);
                            log.warn(
                                    "Gemini transient HTTP {} error. Retrying in {} ms (attempt {}/{})...",
                                    statusCode,
                                    backoff,
                                    attempt,
                                    maxRetries
                            );
                            sleepQuietly(backoff);
                            continue;
                        } else {
                            log.warn(
                                    "Max retries ({}) exhausted for model {} with HTTP {}.",
                                    maxRetries,
                                    activeModel,
                                    statusCode
                            );
                        }
                    } else {
                        // Other non-transient 4xx error: stop retrying this model
                        break;
                    }

                } catch (IllegalStateException ex) {
                    // Propagate validation exceptions directly
                    throw ex;

                } catch (Exception ex) {
                    lastException = ex;
                    log.warn(
                            "Gemini model '{}' attempt {}/{} failed with error: {}. Failing over immediately.",
                            activeModel,
                            attempt,
                            maxRetries,
                            ex.getMessage()
                    );
                    break;
                }
            }
        }

        if (lastException instanceof RestClientResponseException rex) {
            throw new IllegalStateException(
                    "Gemini API request failed with HTTP "
                            + rex.getStatusCode().value()
                            + " after retry and fallback attempts. Check backend logs.",
                    rex
            );
        }

        throw new IllegalStateException(
                "Unable to generate a response from Gemini: "
                        + (lastException != null ? lastException.getMessage() : "All attempts failed"),
                lastException
        );
    }

    // ==========================================
    // CALL GEMINI REST API
    // ==========================================

    private String executeGeminiRequest(String activeModel, Map<String, Object> requestBody) {

        String rawResponse = restClient.post()
                .uri("/v1beta/models/{model}:generateContent", activeModel)
                .header("x-goog-api-key", apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .retrieve()
                .body(String.class);

        if (!StringUtils.hasText(rawResponse)) {
            throw new IllegalStateException("Gemini returned an empty response.");
        }

        JsonNode response;
        try {
            response = objectMapper.readTree(rawResponse);
        } catch (Exception ex) {
            log.error("Failed to parse Gemini JSON response: {}", rawResponse, ex);
            throw new IllegalStateException("Failed to parse Gemini API JSON response: " + ex.getMessage(), ex);
        }

        JsonNode candidates = response.path("candidates");

        if (!candidates.isArray() || candidates.isEmpty()) {
            String blockReason = response
                    .path("promptFeedback")
                    .path("blockReason")
                    .asText("No candidate returned");

            throw new IllegalStateException(
                    "Gemini returned no answer. Reason: " + blockReason
            );
        }

        JsonNode parts = candidates
                .path(0)
                .path("content")
                .path("parts");

        StringBuilder answer = new StringBuilder();

        if (parts.isArray()) {
            for (JsonNode part : parts) {
                if (part.hasNonNull("text")) {
                    answer.append(part.path("text").asText());
                }
            }
        }

        if (answer.isEmpty()) {
            throw new IllegalStateException(
                    "Gemini returned no text in its response."
            );
        }

        log.info(
                "Gemini response generated successfully using model: {}",
                activeModel
        );

        return answer.toString().trim();
    }

    private boolean isTransientStatus(int statusCode) {
        return statusCode == 503
                || statusCode == 429
                || statusCode == 500
                || statusCode == 502
                || statusCode == 504;
    }

    private long calculateBackoff(int attempt) {
        long exponential = retryDelayMs * (1L << (attempt - 1));
        long jitter = (long) (Math.random() * (retryDelayMs / 2.0));
        return exponential + jitter;
    }

    private void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }

    // ==========================================
    // HEALTH CHECK & DIAGNOSTICS
    // ==========================================

    public Map<String, Object> checkHealth() {
        long now = System.currentTimeMillis();
        if (cachedHealth != null && (now - lastHealthCheckTime < HEALTH_CACHE_TTL_MS)) {
            return new LinkedHashMap<>(cachedHealth);
        }

        Map<String, Object> health = new LinkedHashMap<>();
        health.put("apiKeyConfigured", isApiKeyConfigured());
        health.put("primaryModel", primaryModel);
        health.put("fallbackModel", fallbackModel);
        health.put("maxRetries", maxRetries);

        if (!isApiKeyConfigured()) {
            health.put("status", "DOWN");
            health.put("message", "Gemini API key is not configured.");
            return health;
        }

        long start = System.currentTimeMillis();

        try {
            Map<String, Object> pingBody = Map.of(
                    "contents", List.of(
                            Map.of(
                                    "role", "user",
                                    "parts", List.of(Map.of("text", "Ping"))
                            )
                    ),
                    "generationConfig", Map.of(
                            "temperature", 0.0,
                            "maxOutputTokens", 5
                    )
            );

            executeGeminiRequest(primaryModel, pingBody);
            long latency = System.currentTimeMillis() - start;

            health.put("status", "UP");
            health.put("activeModel", primaryModel);
            health.put("latencyMs", latency);
            health.put("message", "Gemini API is healthy and operational.");

        } catch (Exception ex) {
            long latency = System.currentTimeMillis() - start;
            health.put("status", "DEGRADED");
            health.put("latencyMs", latency);
            health.put("message", "Gemini ping probe failed: " + ex.getMessage());
        }

        this.cachedHealth = health;
        this.lastHealthCheckTime = now;
        return health;
    }

    // ==========================================
    // GROUNDING VERIFICATION & ANTI-HALLUCINATION
    // ==========================================

    private static final Pattern CONTEXT_RECALL_ID_PATTERN = Pattern.compile(
            "(?i)\\brecall\\s*(?:id|#)?\\s*#?\\s*:?\\s*(\\d+)\\b"
    );

    private static final Pattern ANSWER_RECALL_ID_PATTERN = Pattern.compile(
            "(?i)\\b(?:recall\\s*(?:id|#)?\\s*#?\\s*|id\\s*#)(\\d+)\\b"
    );

    private static final List<String> FORBIDDEN_SAFETY_PHRASES = List.of(
            "completely safe",
            "totally safe",
            "100% safe",
            "entirely safe",
            "guaranteed safe",
            "is safe to use",
            "free from any defects",
            "free from defects",
            "has no safety issues",
            "has no defects"
    );

    String validateAndEnforceGrounding(String rawAnswer, String recallContext) {

        if (!StringUtils.hasText(rawAnswer)) {
            return rawAnswer;
        }

        // 1. Extract valid Recall IDs from the database context
        Set<String> validContextIds = new HashSet<>();
        if (recallContext != null) {
            Matcher ctxMatcher = CONTEXT_RECALL_ID_PATTERN.matcher(recallContext);
            while (ctxMatcher.find()) {
                validContextIds.add(ctxMatcher.group(1));
            }
        }

        boolean contextHadNoRecalls = validContextIds.isEmpty();
        String validatedAnswer = rawAnswer;
        String lowerAnswer = validatedAnswer.toLowerCase(Locale.ROOT);

        // 2. Check for Hallucinated Recall IDs in the Answer
        Matcher answerMatcher = ANSWER_RECALL_ID_PATTERN.matcher(validatedAnswer);
        Set<String> ungroundedIds = new HashSet<>();

        while (answerMatcher.find()) {
            String citedId = answerMatcher.group(1);
            if (!validContextIds.contains(citedId)) {
                ungroundedIds.add(citedId);
            }
        }

        if (!ungroundedIds.isEmpty()) {
            boolean isNegativeNotice = lowerAnswer.contains("not found")
                    || lowerAnswer.contains("no matching")
                    || lowerAnswer.contains("no record");

            // If the model claimed ungrounded recall IDs affirmatively:
            if (!isNegativeNotice) {
                log.warn(
                        "Hallucination detected! Gemini cited ungrounded Recall ID(s): {}. Valid context IDs: {}",
                        ungroundedIds,
                        validContextIds
                );

                if (contextHadNoRecalls) {
                    log.error(
                            "Severe hallucination: context had 0 recalls, but model fabricated Recall ID(s) {}. Overriding with grounded negative response.",
                            ungroundedIds
                    );

                    return "No matching recall records were found in the RecallRadar database. "
                            + "Please verify safety information directly with the manufacturer or official government portals such as SaferProducts.gov or CPSC.gov. "
                            + "The absence of a database match does not prove that a product is completely safe.";
                } else {
                    for (String badId : ungroundedIds) {
                        validatedAnswer = validatedAnswer.replaceAll(
                                "(?i)\\brecall\\s*(?:id|#)?\\s*#?\\s*" + Pattern.quote(badId) + "\\b",
                                "[Unverified Recall Reference]"
                        );
                    }
                    lowerAnswer = validatedAnswer.toLowerCase(Locale.ROOT);
                }
            }
        }

        // 3. Prevent False Safety Declarations
        boolean containsFalseSafety = FORBIDDEN_SAFETY_PHRASES.stream().anyMatch(lowerAnswer::contains);

        if (containsFalseSafety) {
            log.warn("Unsafe absolute safety claim detected in AI response. Appending mandatory safety advisory.");
            validatedAnswer = validatedAnswer.trim() + "\n\n"
                    + "⚠️ Safety Advisory: Absence of a recall record in the RecallRadar database does not guarantee "
                    + "that a product is completely safe or free from defects. Always verify with the manufacturer or "
                    + "official safety portals such as SaferProducts.gov or CPSC.gov.";
        }

        // 4. Ensure Negative Notice if 0 records and model was ambiguous
        if (contextHadNoRecalls && recallContext != null && recallContext.contains("No matching recall records were found")) {
            if (!lowerAnswer.contains("no matching") && !lowerAnswer.contains("not found") && !lowerAnswer.contains("no recall")) {
                validatedAnswer = "No matching recall records were found in the RecallRadar database.\n\n" + validatedAnswer;
            }
        }

        return validatedAnswer.trim();
    }
}
