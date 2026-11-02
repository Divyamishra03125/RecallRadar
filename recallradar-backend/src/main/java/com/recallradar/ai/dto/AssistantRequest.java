package com.recallradar.ai.dto;

import com.recallradar.ai.ChatMessage;
import java.util.List;

public record AssistantRequest(
        String question,
        List<ChatMessage> conversationHistory,
        String conversationContextId
) {
    public AssistantRequest(String question) {
        this(question, List.of(), null);
    }

    public AssistantRequest(String question, List<ChatMessage> conversationHistory) {
        this(question, conversationHistory != null ? conversationHistory : List.of(), null);
    }
}
