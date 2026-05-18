package com.plog.api.pipeline.dto;

/**
 * Gemini multi-turn 호출용 단일 발화. role은 "user" / "model".
 */
public record ChatTurn(String role, String content) {

    public static ChatTurn user(String content) {
        return new ChatTurn("user", content);
    }

    public static ChatTurn model(String content) {
        return new ChatTurn("model", content);
    }
}
