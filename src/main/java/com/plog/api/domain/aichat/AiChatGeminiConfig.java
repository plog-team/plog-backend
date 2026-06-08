package com.plog.api.domain.aichat;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "aichat.gemini")
public class AiChatGeminiConfig {
    private List<String> apiKeys = new ArrayList<>();  // ← 변경
    private String model = "gemini-2.0-flash";
    private String endpoint = "https://generativelanguage.googleapis.com/v1beta";
    private boolean useMock = false;
}