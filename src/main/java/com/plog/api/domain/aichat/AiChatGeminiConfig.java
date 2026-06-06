package com.plog.api.domain.aichat;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "aichat.gemini")
public class AiChatGeminiConfig {
    private String apiKey;
    private String model = "gemini-1.5-flash";
    private String endpoint = "https://generativelanguage.googleapis.com/v1beta";
    private boolean useMock = false;
}