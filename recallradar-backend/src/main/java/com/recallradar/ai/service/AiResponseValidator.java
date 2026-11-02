package com.recallradar.ai.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class AiResponseValidator {

    private static final Logger log = LoggerFactory.getLogger(AiResponseValidator.class);

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
            "has no defects",
            "certified safe",
            "hazard-free"
    );

    public record ValidationResult(
            boolean valid,
            String validatedAnswer,
            List<String> warnings,
            List<String> missingInformation
    ) {}

    /**
     * Validates the generated answer against the factual database context and anti-hallucination rules.
     */
    public ValidationResult validate(String rawAnswer, String context, boolean contextHadRecalls, com.recallradar.ai.QuestionIntent intent) {
        if (!StringUtils.hasText(rawAnswer)) {
            return new ValidationResult(false, "No response generated.", List.of("Empty AI response"), List.of());
        }

        List<String> warnings = new ArrayList<>();
        List<String> missingInfo = new ArrayList<>();

        // 1. Extract valid Recall IDs from context
        Set<String> validContextIds = new HashSet<>();
        if (context != null) {
            Matcher ctxMatcher = CONTEXT_RECALL_ID_PATTERN.matcher(context);
            while (ctxMatcher.find()) {
                validContextIds.add(ctxMatcher.group(1));
            }
        }

        String validated = rawAnswer;
        String lower = validated.toLowerCase(Locale.ROOT);

        // 2. Check for Hallucinated Recall IDs
        Matcher answerMatcher = ANSWER_RECALL_ID_PATTERN.matcher(validated);
        Set<String> ungroundedIds = new HashSet<>();
        while (answerMatcher.find()) {
            String citedId = answerMatcher.group(1);
            if (!validContextIds.contains(citedId)) {
                ungroundedIds.add(citedId);
            }
        }

        if (!ungroundedIds.isEmpty()) {
            boolean isNegativeNotice = lower.contains("not found")
                    || lower.contains("no matching")
                    || lower.contains("no record");

            if (!isNegativeNotice) {
                log.warn("Hallucination detected! Cited ungrounded Recall ID(s): {}. Valid context IDs: {}",
                        ungroundedIds, validContextIds);

                if (!contextHadRecalls) {
                    log.error("Severe hallucination: context had 0 recalls, but model fabricated Recall ID(s) {}. Overriding with grounded negative response.",
                            ungroundedIds);
                    return new ValidationResult(
                            false,
                            "No matching recall records were found in the current RecallRadar database. "
                                    + "Please verify safety information directly with the manufacturer or official government portals such as SaferProducts.gov or CPSC.gov. "
                                    + "The absence of a database match does not prove that a product is completely safe.",
                            List.of("Severe hallucination prevented: ungrounded Recall IDs fabricated."),
                            List.of()
                    );
                } else {
                    for (String badId : ungroundedIds) {
                        validated = validated.replaceAll(
                                "(?i)\\brecall\\s*(?:id|#)?\\s*#?\\s*" + Pattern.quote(badId) + "\\b",
                                "[Unverified Recall Reference]"
                        );
                    }
                    warnings.add("Removed unverified recall reference(s): " + ungroundedIds);
                    lower = validated.toLowerCase(Locale.ROOT);
                }
            }
        }

        // 3. Prevent False Safety Declarations
        boolean containsFalseSafety = FORBIDDEN_SAFETY_PHRASES.stream().anyMatch(lower::contains);
        if (containsFalseSafety) {
            log.warn("Unsafe absolute safety claim detected in AI response. Appending mandatory safety advisory.");
            validated = validated.trim() + "\n\n"
                    + "⚠️ Safety Advisory: Absence of a recall record in the RecallRadar database does not guarantee "
                    + "that a product is completely safe or free from defects. Always verify with the manufacturer or "
                    + "official safety portals such as SaferProducts.gov or CPSC.gov.";
            warnings.add("Safety advisory appended to mitigate absolute safety assertion.");
        }

        // 4. Ensure Negative Notice only for product/recall check queries where 0 records were found
        boolean isSpecificRecallQuery = intent == com.recallradar.ai.QuestionIntent.PRODUCT_SEARCH
                || intent == com.recallradar.ai.QuestionIntent.PRODUCT_RECALL_CHECK
                || intent == com.recallradar.ai.QuestionIntent.MODEL_SEARCH
                || intent == com.recallradar.ai.QuestionIntent.BATCH_SEARCH
                || intent == com.recallradar.ai.QuestionIntent.RECALL_ID_SEARCH
                || intent == com.recallradar.ai.QuestionIntent.RECALL_SEARCH;

        if (isSpecificRecallQuery && !contextHadRecalls && context != null && context.contains("No matching recall records were found in the RecallRadar database")) {
            if (!lower.contains("no matching") && !lower.contains("not found") && !lower.contains("no recall")) {
                validated = "No matching recall records were found in the RecallRadar database.\n\n" + validated;
            }
            warnings.add("No matching recall records in database.");
        }

        return new ValidationResult(true, validated.trim(), warnings, missingInfo);
    }
}
