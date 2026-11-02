package com.recallradar.ai.dto;

import java.util.List;
import java.util.Map;

public record AssistantResponse(
        String answer,
        String intent,
        String matchStatus,
        List<Map<String, Object>> products,
        List<Map<String, Object>> recalls,
        List<Map<String, Object>> sources,
        List<String> warnings,
        List<String> suggestedFollowUps,
        String sourceType,
        List<String> missingInformation,
        String aiStatus,
        String conversationContextId
) {
    public AssistantResponse(String answer) {
        this(answer, "UNKNOWN", "NO_MATCH", List.of(), List.of(), List.of(), List.of(), List.of(),
                "DATABASE_DIRECT", List.of(), "ONLINE", null);
    }

    public AssistantResponse(String answer, List<String> suggestedFollowUps) {
        this(answer, "UNKNOWN", "NO_MATCH", List.of(), List.of(), List.of(), List.of(), suggestedFollowUps,
                "DATABASE_DIRECT", List.of(), "ONLINE", null);
    }

    // 8-argument constructor preserving existing callers and tests
    public AssistantResponse(
            String answer,
            String intent,
            String matchStatus,
            List<Map<String, Object>> products,
            List<Map<String, Object>> recalls,
            List<Map<String, Object>> sources,
            List<String> warnings,
            List<String> suggestedFollowUps
    ) {
        this(answer, intent, matchStatus, products, recalls, sources, warnings, suggestedFollowUps,
                "DATABASE_DIRECT", List.of(), "ONLINE", null);
    }

    public AssistantResponse {
        answer = answer != null ? answer : "";
        intent = intent != null ? intent : "UNKNOWN";
        matchStatus = matchStatus != null ? matchStatus : "NO_MATCH";
        products = products != null ? products : List.of();
        recalls = recalls != null ? recalls : List.of();
        sources = sources != null ? sources : List.of();
        warnings = warnings != null ? warnings : List.of();
        suggestedFollowUps = suggestedFollowUps != null ? suggestedFollowUps : List.of();
        sourceType = sourceType != null ? sourceType : "DATABASE_DIRECT";
        missingInformation = missingInformation != null ? missingInformation : List.of();
        aiStatus = aiStatus != null ? aiStatus : "ONLINE";
    }
}
