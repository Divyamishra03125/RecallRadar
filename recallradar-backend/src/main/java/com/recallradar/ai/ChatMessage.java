package com.recallradar.ai;

import org.springframework.util.StringUtils;

public record ChatMessage(
        String role,
        String content
) {
    public ChatMessage {
        if (!StringUtils.hasText(role)) {
            role = "user";
        }
        if (content == null) {
            content = "";
        }
    }

    public static ChatMessage user(String content) {
        return new ChatMessage("user", content);
    }

    public static ChatMessage assistant(String content) {
        return new ChatMessage("assistant", content);
    }
}
