package com.plog.api.domain.aichat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class AiChatGeminiClient {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final AiChatGeminiConfig config;

    public String chat(String systemPrompt, List<Map<String, Object>> history, String userMessage) {
        List<Map<String, Object>> contents = new java.util.ArrayList<>(history);
        contents.add(Map.of(
            "role", "user",
            "parts", List.of(Map.of("text", userMessage))
        ));

        Map<String, Object> body = Map.of(
            "system_instruction", Map.of(
                "parts", List.of(Map.of("text", systemPrompt))
            ),
            "contents", contents,
            "generationConfig", Map.of(
                "temperature", 0.7,
                "maxOutputTokens", 1024
            )
        );

        String url = config.getEndpoint() + "/models/" + config.getModel()
                + ":generateContent?key=" + config.getApiKey();
        
        log.info("Gemini 호출 URL: {}", url);

        try {
            String resp = webClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            JsonNode root = objectMapper.readTree(resp);
            return root.path("candidates").path(0)
                    .path("content").path("parts").path(0)
                    .path("text").asText("");
        } catch (Exception e) {
            log.error("AiChat Gemini 호출 실패: {}", e.getMessage());
            throw new RuntimeException("AI 응답 생성 실패: " + e.getMessage());
        }
    }
}