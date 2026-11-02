package com.recallradar.ai.service;

import com.recallradar.ai.QuestionClassification;
import com.recallradar.ai.QuestionClassifier;
import com.recallradar.ai.QuestionIntent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class QueryIntentClassifier {

    private final QuestionClassifier baseClassifier;

    private static final Pattern DATE_RANGE_PATTERN = Pattern.compile(
            "(?i)\\b(?:between|from)\\s+(\\d{4}-\\d{2}-\\d{2})\\s+(?:and|to)\\s+(\\d{4}-\\d{2}-\\d{2})\\b"
    );

    private static final Pattern YEAR_PATTERN = Pattern.compile(
            "(?i)\\b(?:in|during|year)\\s+(20\\d{2})\\b"
    );

    private static final Pattern SOURCE_PATTERN = Pattern.compile(
            "(?i)\\b(cpsc|fda|nhtsa|europa|recalls\\.gov|source|regulatory\\s+authority|official\\s+link)\\b"
    );

    @Autowired
    public QueryIntentClassifier(QuestionClassifier baseClassifier) {
        this.baseClassifier = baseClassifier != null ? baseClassifier : new QuestionClassifier();
    }

    public QueryIntentClassifier() {
        this(new QuestionClassifier());
    }

    public QuestionClassification classify(String question) {
        if (!StringUtils.hasText(question)) {
            return QuestionClassification.of(QuestionIntent.HELP);
        }

        String raw = question.trim();
        String lower = raw.toLowerCase(Locale.ROOT);

        // 1. Run base classifier for core heuristics
        QuestionClassification base = baseClassifier.classify(raw);
        QuestionIntent intent = base.intent();

        // 2. Extract date range if present
        LocalDate dateFrom = null;
        LocalDate dateTo = null;
        Matcher dateMatcher = DATE_RANGE_PATTERN.matcher(raw);
        if (dateMatcher.find()) {
            try {
                dateFrom = LocalDate.parse(dateMatcher.group(1));
                dateTo = LocalDate.parse(dateMatcher.group(2));
            } catch (Exception ignored) {}
        } else {
            Matcher yearMatcher = YEAR_PATTERN.matcher(raw);
            if (yearMatcher.find()) {
                int yr = Integer.parseInt(yearMatcher.group(1));
                dateFrom = LocalDate.of(yr, 1, 1);
                dateTo = LocalDate.of(yr, 12, 31);
            }
        }

        // 3. Extract requested operation / source lookup
        String requestedOperation = null;
        if (lower.contains("compare") || lower.contains("versus") || lower.contains("vs")) {
            requestedOperation = "COMPARE";
        } else if (lower.contains("count") || lower.contains("how many")) {
            requestedOperation = "COUNT";
        } else if (lower.contains("list") || lower.contains("show")) {
            requestedOperation = "LIST";
        } else if (lower.contains("explain") || lower.contains("why")) {
            requestedOperation = "EXPLAIN";
        }

        // 4. Canonical intent refinements
        if (lower.contains("source") || lower.contains("cpsc") || lower.contains("fda") || lower.contains("regulatory link")) {
            if (base.recallId() != null || base.hasSpecificIdentifier()) {
                intent = QuestionIntent.SOURCE_LOOKUP;
            }
        }

        if (lower.contains("model search")) {
            intent = QuestionIntent.MODEL_SEARCH;
        } else if (lower.contains("batch search")) {
            intent = QuestionIntent.BATCH_SEARCH;
        } else if (lower.contains("severity search")) {
            intent = QuestionIntent.SEVERITY_SEARCH;
        } else if (lower.contains("status search")) {
            intent = QuestionIntent.STATUS_SEARCH;
        } else if (lower.contains("recall id search")) {
            intent = QuestionIntent.RECALL_ID_SEARCH;
        } else if (intent == QuestionIntent.UNKNOWN || intent == QuestionIntent.UNKNOWN_OR_UNSUPPORTED) {
            intent = QuestionIntent.UNSUPPORTED;
        }

        // Extract productName heuristic (e.g., brand + terms)
        String productName = base.productName();
        if (productName == null && base.brand() != null) {
            productName = extractProductNameCandidate(raw, base.brand());
        }

        return new QuestionClassification(
                intent,
                base.recallId(),
                base.productId(),
                productName,
                base.brand(),
                base.modelNumber(),
                base.batchNumber(),
                base.severity(),
                base.targetStatus(),
                dateFrom,
                dateTo,
                requestedOperation,
                base.hasSpecificIdentifier(),
                base.clarificationPrompt()
        );
    }

    private String extractProductNameCandidate(String raw, String brand) {
        String lower = raw.toLowerCase(Locale.ROOT);
        int idx = lower.indexOf(brand.toLowerCase(Locale.ROOT));
        if (idx == -1) return null;

        String sub = raw.substring(idx).trim();
        // Cut off trailing question words
        sub = sub.replaceAll("(?i)\\s+(has|have|had|with|is|are|recalled|recalls|recall|recalled\\?|\\?).*$", "");
        return StringUtils.hasText(sub) ? sub.trim() : brand;
    }
}
