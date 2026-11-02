package com.recallradar.ai.service;

import com.recallradar.ai.ChatMessage;
import com.recallradar.ai.QuestionClassification;
import com.recallradar.ai.QuestionIntent;
import com.recallradar.product.Product;
import com.recallradar.recall.Recall;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ConversationContextService {

    private static final Logger log = LoggerFactory.getLogger(ConversationContextService.class);

    private static final long SESSION_TTL_MILLIS = 3600_000L; // 1 hour

    public record ConversationState(
            String contextId,
            QuestionIntent lastIntent,
            Long lastProductId,
            String lastProductName,
            String lastBrand,
            String lastModelNumber,
            String lastBatchNumber,
            Long lastRecallId,
            List<Long> lastResultProductIds,
            List<Long> lastResultRecallIds,
            Instant lastActiveAt
    ) {
        public ConversationState withActivity(Instant now) {
            return new ConversationState(
                    contextId, lastIntent, lastProductId, lastProductName, lastBrand,
                    lastModelNumber, lastBatchNumber, lastRecallId,
                    lastResultProductIds, lastResultRecallIds, now
            );
        }
    }

    private final Map<String, ConversationState> sessions = new ConcurrentHashMap<>();

    private static final Pattern RECALL_ID_PATTERN = Pattern.compile(
            "(?i)\\brecall\\s*(?:id|#)?\\s*#?\\s*:?\\s*(\\d+)\\b"
    );

    /**
     * Retrieves the stored conversation state for a given context ID, cleaning up expired sessions.
     */
    public Optional<ConversationState> getState(String contextId) {
        if (!StringUtils.hasText(contextId)) return Optional.empty();
        ConversationState state = sessions.get(contextId);
        if (state == null) return Optional.empty();

        if (Instant.now().toEpochMilli() - state.lastActiveAt().toEpochMilli() > SESSION_TTL_MILLIS) {
            sessions.remove(contextId);
            return Optional.empty();
        }
        return Optional.of(state);
    }

    /**
     * Updates the conversation state with newly resolved entities and results.
     */
    public void updateState(
            String contextId,
            QuestionIntent intent,
            List<Product> products,
            List<Recall> recalls,
            QuestionClassification classification
    ) {
        if (!StringUtils.hasText(contextId)) return;

        List<Long> productIds = products != null ? products.stream().map(Product::getId).filter(Objects::nonNull).toList() : List.of();
        List<Long> recallIds = recalls != null ? recalls.stream().map(Recall::getId).filter(Objects::nonNull).toList() : List.of();

        Long currentProductId = !productIds.isEmpty() ? productIds.get(0) : (classification != null && classification.hasSpecificIdentifier() ? null : null);
        String currentProductName = (products != null && !products.isEmpty()) ? products.get(0).getName() : null;
        String currentBrand = (products != null && !products.isEmpty() && StringUtils.hasText(products.get(0).getBrand()))
                ? products.get(0).getBrand()
                : (classification != null ? classification.brand() : null);
        String currentModel = (products != null && !products.isEmpty() && StringUtils.hasText(products.get(0).getModelNumber()))
                ? products.get(0).getModelNumber()
                : (classification != null ? classification.modelNumber() : null);
        String currentBatch = (products != null && !products.isEmpty() && StringUtils.hasText(products.get(0).getBatchNumber()))
                ? products.get(0).getBatchNumber()
                : (classification != null ? classification.batchNumber() : null);

        Long currentRecallId = (classification != null && classification.recallId() != null)
                ? classification.recallId()
                : (!recallIds.isEmpty() ? recallIds.get(0) : null);

        // Check if previous state exists
        ConversationState prev = sessions.get(contextId);
        if (prev != null) {
            // If new question introduces a completely different brand or product, clear old IDs
            if (StringUtils.hasText(currentBrand) && StringUtils.hasText(prev.lastBrand())
                    && !currentBrand.equalsIgnoreCase(prev.lastBrand())) {
                log.info("Conversation {} switched subject from '{}' to '{}'. Resetting context.",
                        contextId, prev.lastBrand(), currentBrand);
                prev = null;
            }
        }

        ConversationState newState = new ConversationState(
                contextId,
                intent != null ? intent : (prev != null ? prev.lastIntent() : QuestionIntent.UNKNOWN),
                currentProductId != null ? currentProductId : (prev != null ? prev.lastProductId() : null),
                currentProductName != null ? currentProductName : (prev != null ? prev.lastProductName() : null),
                currentBrand != null ? currentBrand : (prev != null ? prev.lastBrand() : null),
                currentModel != null ? currentModel : (prev != null ? prev.lastModelNumber() : null),
                currentBatch != null ? currentBatch : (prev != null ? prev.lastBatchNumber() : null),
                currentRecallId != null ? currentRecallId : (prev != null ? prev.lastRecallId() : null),
                !productIds.isEmpty() ? productIds : (prev != null ? prev.lastResultProductIds() : List.of()),
                !recallIds.isEmpty() ? recallIds : (prev != null ? prev.lastResultRecallIds() : List.of()),
                Instant.now()
        );

        sessions.put(contextId, newState);
    }

    /**
     * Resolves follow-up pronouns and elliptical queries against structured session state
     * or recent chat history.
     */
    public String contextualizeQuery(String rawQuestion, String contextId, List<ChatMessage> history) {
        if (!StringUtils.hasText(rawQuestion)) return "";
        String trimmed = rawQuestion.trim();
        String lower = trimmed.toLowerCase(Locale.ROOT);

        // Check for follow-up indicators
        boolean isFollowUp = lower.contains("it")
                || lower.contains("this product")
                || lower.contains("that product")
                || lower.contains("this recall")
                || lower.contains("that recall")
                || lower.contains("the recall")
                || lower.contains("its batch")
                || lower.contains("its model")
                || lower.contains("its severity")
                || lower.contains("its hazard")
                || lower.contains("the critical one")
                || lower.contains("the high one")
                || lower.contains("the first one")
                || lower.contains("the second one")
                || lower.contains("what about")
                || lower.contains("is it recalled")
                || lower.contains("does it have any recalls")
                || lower.contains("explain it")
                || lower.contains("explain the critical");

        if (!isFollowUp) {
            return trimmed;
        }

        // 1. First attempt resolution via structured state
        Optional<ConversationState> optState = getState(contextId);
        if (optState.isPresent()) {
            ConversationState s = optState.get();

            // "the critical one" / "the first one"
            if ((lower.contains("critical") || lower.contains("first")) && s.lastRecallId() != null) {
                return trimmed + " (referring to Recall ID #" + s.lastRecallId() + ")";
            }

            // "its model" / "its batch" / "is it recalled"
            if (s.lastProductId() != null || StringUtils.hasText(s.lastProductName())) {
                String ref = StringUtils.hasText(s.lastProductName()) ? s.lastProductName() : "Product #" + s.lastProductId();
                return trimmed + " (referring to " + ref + ")";
            }

            if (StringUtils.hasText(s.lastBrand())) {
                return trimmed + " (referring to " + s.lastBrand() + ")";
            }
        }

        // 2. Fall back to inspection of previous chat messages in history
        if (history != null && !history.isEmpty()) {
            for (int i = history.size() - 1; i >= Math.max(0, history.size() - 6); i--) {
                ChatMessage msg = history.get(i);
                if (msg == null || !StringUtils.hasText(msg.content())) continue;
                String content = msg.content();

                Matcher idMatcher = RECALL_ID_PATTERN.matcher(content);
                if (idMatcher.find()) {
                    return trimmed + " (referring to Recall ID #" + idMatcher.group(1) + ")";
                }

                // Check known product mentions in previous messages
                for (String brand : List.of("samsung", "oppo", "redmi", "xiaomi", "apple", "brandx", "primark", "decathlon", "papablic", "dr power")) {
                    if (content.toLowerCase(Locale.ROOT).contains(brand)) {
                        return trimmed + " (referring to " + brand + ")";
                    }
                }
            }
        }

        return trimmed;
    }

    /**
     * Clears session state on user request or conversation reset.
     */
    public void clearSession(String contextId) {
        if (StringUtils.hasText(contextId)) {
            sessions.remove(contextId);
        }
    }
}
