package com.recallradar.ai;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class QuestionClassifier {

    private static final Pattern EXPLICIT_MODEL_PATTERN = Pattern.compile(
            "(?i)\\b(?:model\\s*(?:number|no|#)|model|mod|m/n)\\s*[:#=-]?\\s*([a-z0-9]+(?:[-_/][a-z0-9]+)*)"
    );

    private static final Pattern EXPLICIT_BATCH_PATTERN = Pattern.compile(
            "(?i)\\b(?:batch\\s*(?:number|no|#)|batch|lot\\s*(?:number|no|#)|lot|b/n)\\s*[:#=-]?\\s*([a-z0-9]+(?:[-_/][a-z0-9]+)*)"
    );

    private static final Pattern PRODUCT_ID_PATTERN = Pattern.compile(
            "(?i)\\bproduct\\s*(?:id|#)?\\s*#?\\s*:?\\s*(\\d+)\\b"
    );

    private static final Pattern ALPHANUMERIC_IDENTIFIER_PATTERN = Pattern.compile(
            "(?i)\\b(?=[a-z0-9_-]*[a-z])(?=[a-z0-9_-]*\\d)[a-z0-9]+(?:[-_/][a-z0-9]+)*\\b"
    );

    private static final Pattern RECALL_ID_PATTERN = Pattern.compile(
            "(?i)\\brecall\\s*(?:id|#)?\\s*#?\\s*(\\d+)\\b"
    );

    private static final Set<String> IDENTIFIER_STOPWORDS = Set.of(
            "number", "numbers", "no", "code", "codes", "what", "is", "its", "the", "for", "details", "check", "recalls", "recall"
    );

    private static final String[] KNOWN_BRANDS = {
            "samsung", "oppo", "redmi", "xiaomi", "apple", "brandx", "primark", "decathlon", "papablic", "dr power", "sony", "bosch", "dewalt"
    };

    public QuestionClassification classify(String question) {
        if (!StringUtils.hasText(question)) {
            return QuestionClassification.of(QuestionIntent.GREETING_OR_CHITCHAT);
        }

        String raw = question.trim();
        String lower = raw.toLowerCase(Locale.ROOT);

        // 1. GREETING & CHITCHAT
        if (isGreetingOrChitchat(lower)) {
            return QuestionClassification.of(QuestionIntent.GREETING_OR_CHITCHAT);
        }

        // 2. APPLICATION HELP & CAPABILITIES
        if (isHelpQuestion(lower)) {
            return QuestionClassification.of(QuestionIntent.APPLICATION_HELP);
        }

        // 3. DASHBOARD & OVERVIEW ANALYSIS / DATABASE STATISTICS
        if (isDashboardOverviewQuestion(lower)) {
            String targetStatus = resolveRequestedStatus(lower);
            return new QuestionClassification(
                    QuestionIntent.DASHBOARD_ANALYSIS,
                    null, null, null, null, null, null, targetStatus, false, null
            );
        }

        // 4. EXTRACT IDENTIFIERS & ENTITIES
        Long recallId = extractRecallId(raw);
        Long productId = extractProductId(raw);
        String modelNumber = extractModelIdentifier(raw);
        String batchNumber = extractBatchIdentifier(raw);
        String extractedBrand = extractBrand(lower);
        String extractedSeverity = extractSeverity(lower);
        String targetStatus = resolveRequestedStatus(lower);
        boolean hasIdentifier = recallId != null || productId != null || modelNumber != null || batchNumber != null || hasSpecificIdentifier(raw);

        // 5. RECALL DETAILS (Explicit Recall ID)
        if (recallId != null || (isRecallDetailQuestion(lower) && hasIdentifier)) {
            return new QuestionClassification(
                    QuestionIntent.RECALL_DETAILS,
                    recallId, productId, null, extractedBrand, modelNumber, batchNumber, extractedSeverity, targetStatus, true, null
            );
        }

        // 6. UNSPECIFIED RECALL DETAIL (prompts for clarification)
        if (isRecallDetailQuestion(lower) && !hasIdentifier) {
            return new QuestionClassification(
                    QuestionIntent.RECALL_DETAILS,
                    null, null, null, null, null, null, null, targetStatus, false,
                    "Which recall are you asking about? Please provide a Recall ID, product name, brand, model number or batch code."
            );
        }

        // 7. PRODUCT COMPARISON (e.g., "compare Samsung and Oppo")
        if (isComparisonQuestion(lower)) {
            return new QuestionClassification(
                    QuestionIntent.PRODUCT_COMPARISON,
                    recallId, productId, null, extractedBrand, modelNumber, batchNumber, extractedSeverity, targetStatus, hasIdentifier, null
            );
        }

        // 8. BATCH INVESTIGATION (Explicit Batch inquiry)
        if (isBatchCheckQuestion(lower, batchNumber)) {
            return new QuestionClassification(
                    QuestionIntent.BATCH_INVESTIGATION,
                    recallId, productId, null, extractedBrand, modelNumber, batchNumber, extractedSeverity, targetStatus, true, null
            );
        }

        // 9. REPORT ASSISTANCE
        if (isReportQuestion(lower)) {
            return new QuestionClassification(
                    QuestionIntent.REPORT_ASSISTANCE,
                    recallId, productId, null, extractedBrand, modelNumber, batchNumber, extractedSeverity, targetStatus, hasIdentifier, null
            );
        }

        // 10. PRODUCT DETAILS (Explicit Product ID or specs inquiry)
        if (productId != null && !isProductRecallCheckQuestion(lower, hasIdentifier, extractedBrand)) {
            return new QuestionClassification(
                    QuestionIntent.PRODUCT_DETAILS,
                    null, productId, null, extractedBrand, modelNumber, batchNumber, extractedSeverity, targetStatus, true, null
            );
        }
        if (isProductDetailsQuestion(lower, hasIdentifier)) {
            return new QuestionClassification(
                    QuestionIntent.PRODUCT_DETAILS,
                    null, productId, null, extractedBrand, modelNumber, batchNumber, extractedSeverity, targetStatus, hasIdentifier, null
            );
        }

        // 11. RISK ANALYSIS (e.g. "which products have highest risk", "highest recall risk")
        if (isRiskAnalysisQuestion(lower)) {
            return new QuestionClassification(
                    QuestionIntent.RISK_ANALYSIS,
                    recallId, productId, null, extractedBrand, modelNumber, batchNumber, extractedSeverity, targetStatus, hasIdentifier, null
            );
        }

        // 12. SEVERITY ANALYSIS
        if (isSeverityAnalysisQuestion(lower) && recallId == null) {
            return new QuestionClassification(
                    QuestionIntent.SEVERITY_ANALYSIS,
                    null, productId, null, extractedBrand, modelNumber, batchNumber, extractedSeverity, targetStatus, hasIdentifier, null
            );
        }

        // 13. GENERAL SAFETY GUIDANCE
        if (isGeneralSafetyQuestion(lower) && !hasIdentifier && extractedBrand == null) {
            return new QuestionClassification(
                    QuestionIntent.GENERAL_SAFETY,
                    null, null, null, null, null, null, null, targetStatus, false, null
            );
        }

        // 14. PRODUCT RECALL CHECK (Asking if product/model/batch is recalled or affected)
        if (isProductRecallCheckQuestion(lower, hasIdentifier, extractedBrand)) {
            return new QuestionClassification(
                    QuestionIntent.PRODUCT_RECALL_CHECK,
                    null, productId, null, extractedBrand, modelNumber, batchNumber, extractedSeverity, targetStatus, hasIdentifier, null
            );
        }

        // 15. PRODUCT CATALOGUE SEARCH (Looking for registered products without recall question)
        if (isProductSearchQuestion(lower, extractedBrand, modelNumber, batchNumber)) {
            return new QuestionClassification(
                    QuestionIntent.PRODUCT_SEARCH,
                    null, productId, null, extractedBrand, modelNumber, batchNumber, extractedSeverity, targetStatus, hasIdentifier, null
            );
        }

        // 16. RECALL SEARCH (Default search across recall records)
        return new QuestionClassification(
                QuestionIntent.RECALL_SEARCH,
                recallId, productId, null, extractedBrand, modelNumber, batchNumber, extractedSeverity, targetStatus, hasIdentifier, null
        );
    }

    public boolean isGreetingOrChitchat(String lower) {
        if (!StringUtils.hasText(lower)) return true;
        String trimmed = lower.trim();
        return trimmed.matches("^(hi+|hello+|hey+|howdy|hola|greetings)(\\s+.*)?$")
                || trimmed.matches("^(good\\s+(morning|afternoon|evening|day))(\\s+.*)?$")
                || trimmed.equals("what's up")
                || trimmed.equals("whats up")
                || trimmed.equals("yo");
    }

    public boolean isHelpQuestion(String lower) {
        return lower.contains("what can you do")
                || lower.contains("who are you")
                || lower.contains("how do you work")
                || lower.contains("how can i use recallradar")
                || lower.contains("what can i search")
                || lower.contains("what are your capabilities")
                || lower.equals("help")
                || lower.contains("what is recallradar");
    }

    public boolean isDashboardOverviewQuestion(String lower) {
        if (lower.contains("recall id") || lower.contains("referring to") || lower.contains("does it")
                || lower.contains("is it") || lower.contains("this product") || lower.contains("that product")
                || lower.contains("this recall") || lower.contains("that recall")) {
            return false;
        }
        if (lower.startsWith("show ") || lower.startsWith("list ") || lower.startsWith("find ") || lower.startsWith("search ")) {
            return false;
        }
        return lower.contains("how many recalls")
                || lower.contains("how many products")
                || lower.contains("total recalls")
                || lower.contains("total products")
                || lower.contains("database statistics")
                || lower.contains("count of recalls")
                || lower.contains("overview of recalls")
                || lower.contains("database summary")
                || lower.contains("how many active")
                || (lower.contains("active recalls") && !lower.contains("samsung") && !lower.contains("for") && !lower.contains("model"))
                || lower.contains("how many critical")
                || lower.contains("platform stats");
    }

    public boolean isReportQuestion(String lower) {
        return lower.contains("report")
                && (lower.contains("generate") || lower.contains("export") || lower.contains("download")
                    || lower.contains("create") || lower.contains("pdf") || lower.contains("csv")
                    || lower.contains("compliance") || lower.contains("safety summary") || lower.contains("assistance"));
    }

    public boolean isProductDetailsQuestion(String lower, boolean hasIdentifier) {
        return (lower.contains("product detail")
                || lower.contains("details of product")
                || lower.contains("specs of")
                || lower.contains("specifications")
                || lower.contains("product info")
                || lower.contains("tell me about the product")
                || lower.contains("about product"))
                && hasIdentifier;
    }

    public boolean isComparisonQuestion(String lower) {
        return lower.contains("compare ")
                || lower.contains(" vs ")
                || lower.contains(" versus ")
                || lower.contains("difference between ");
    }

    public boolean isBatchCheckQuestion(String lower, String batchNumber) {
        return (batchNumber != null && !batchNumber.isBlank())
                || lower.contains("check batch")
                || lower.contains("batch number")
                || lower.contains("lot number")
                || lower.contains("batch check");
    }

    public boolean isRiskAnalysisQuestion(String lower) {
        return lower.contains("highest risk")
                || lower.contains("highest recall risk")
                || lower.contains("risk analysis")
                || lower.contains("most recalls")
                || lower.contains("most dangerous")
                || lower.contains("top risk")
                || lower.contains("risk score");
    }

    public boolean isSeverityAnalysisQuestion(String lower) {
        return (lower.contains("severity") && (lower.contains("distribution") || lower.contains("all") || lower.contains("breakdown") || lower.contains("tier")))
                || lower.contains("high severity recalls")
                || lower.contains("critical recalls")
                || lower.contains("medium severity")
                || lower.contains("low severity")
                || lower.contains("unknown severity");
    }

    public boolean isGeneralSafetyQuestion(String lower) {
        return lower.contains("how can i check")
                || lower.contains("what should i check")
                || lower.contains("before buying")
                || lower.contains("used product")
                || lower.contains("used crib")
                || lower.contains("used car")
                || lower.contains("general safety")
                || lower.contains("safety tips")
                || lower.contains("product safety advice")
                || lower.contains("what is a product recall")
                || lower.contains("what does an active recall mean");
    }

    public boolean isRecallDetailQuestion(String lower) {
        return lower.contains("explain recall")
                || lower.contains("recall details")
                || lower.contains("details of the recall")
                || lower.contains("when was the recall issued")
                || lower.contains("what is the severity of the recall")
                || lower.contains("what was the reason for the recall")
                || lower.contains("summarize recall");
    }

    public boolean isProductRecallCheckQuestion(String lower, boolean hasIdentifier, String brand) {
        return lower.contains("recalled")
                || lower.contains("has any recall")
                || lower.contains("is recalled")
                || lower.contains("are recalled")
                || lower.contains("any recall")
                || lower.contains("check if")
                || (lower.contains("check ") && (hasIdentifier || brand != null))
                || lower.contains("affected by");
    }

    public boolean isProductSearchQuestion(String lower, String brand, String model, String batch) {
        if (brand != null && (lower.equals(brand) || lower.startsWith(brand + " ") || lower.endsWith(" " + brand))) {
            return true;
        }
        return lower.contains("find all")
                || lower.contains("find ")
                || lower.contains("list products")
                || lower.contains("show products")
                || lower.contains("search product")
                || lower.contains("search for")
                || lower.contains("catalogue")
                || lower.equals("laptop")
                || lower.equals("phone")
                || lower.equals("smartphone")
                || lower.equals("electronics")
                || lower.equals("appliances")
                || (lower.startsWith("find") && brand != null)
                || (model != null && !lower.contains("recall"));
    }

    public Long extractRecallId(String raw) {
        if (!StringUtils.hasText(raw)) return null;
        Matcher m = RECALL_ID_PATTERN.matcher(raw);
        if (m.find()) {
            try {
                return Long.parseLong(m.group(1));
            } catch (NumberFormatException ignored) {}
        }
        return null;
    }

    public Long extractProductId(String raw) {
        if (!StringUtils.hasText(raw)) return null;
        Matcher m = PRODUCT_ID_PATTERN.matcher(raw);
        if (m.find()) {
            try {
                return Long.parseLong(m.group(1));
            } catch (NumberFormatException ignored) {}
        }
        return null;
    }

    public String extractModelIdentifier(String raw) {
        if (!StringUtils.hasText(raw)) return null;
        Matcher explicit = EXPLICIT_MODEL_PATTERN.matcher(raw);
        if (explicit.find()) {
            String val = explicit.group(1).trim();
            if (!IDENTIFIER_STOPWORDS.contains(val.toLowerCase(Locale.ROOT))) {
                return val;
            }
        }
        return null;
    }

    public String extractBatchIdentifier(String raw) {
        if (!StringUtils.hasText(raw)) return null;
        Matcher explicit = EXPLICIT_BATCH_PATTERN.matcher(raw);
        if (explicit.find()) {
            String val = explicit.group(1).trim();
            if (!IDENTIFIER_STOPWORDS.contains(val.toLowerCase(Locale.ROOT))) {
                return val;
            }
        }
        return null;
    }

    public String extractBrand(String lower) {
        if (!StringUtils.hasText(lower)) return null;
        for (String b : KNOWN_BRANDS) {
            if (lower.contains(b)) {
                return b;
            }
        }
        return null;
    }

    public String extractSeverity(String lower) {
        if (!StringUtils.hasText(lower)) return null;
        if (lower.contains("critical")) return "CRITICAL";
        if (lower.contains("high")) return "HIGH";
        if (lower.contains("medium") || lower.contains("moderate")) return "MEDIUM";
        if (lower.contains("low")) return "LOW";
        if (lower.contains("unknown")) return "UNKNOWN";
        return null;
    }

    public String resolveRequestedStatus(String lower) {
        if (!StringUtils.hasText(lower)) return null;
        if (lower.contains("inactive") || lower.contains("resolved") || lower.contains("closed")) return "INACTIVE";
        if (lower.contains("active")) return "ACTIVE";
        return null;
    }

    public boolean hasSpecificIdentifier(String raw) {
        if (!StringUtils.hasText(raw)) return false;
        Matcher m = ALPHANUMERIC_IDENTIFIER_PATTERN.matcher(raw);
        return m.find();
    }
}
