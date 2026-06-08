package com.plog.api.domain.aichat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
@RequiredArgsConstructor
public class AiChatGeminiClient {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final AiChatGeminiConfig config;

    private final AtomicInteger keyIndex = new AtomicInteger(0);

    private String getNextKey() {
        List<String> keys = config.getApiKeys();
        if (keys.isEmpty()) throw new RuntimeException("API 키 없음");
        int idx = keyIndex.getAndIncrement() % keys.size();
        return keys.get(idx);
    }

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
                + ":generateContent?key=" + getNextKey();  // ← getNextKey() 사용

        log.info("Gemini 호출 URL: {}", url);

        
        try {
            String resp = webClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            log.info("Gemini 응답 = {}", resp);

            JsonNode root = objectMapper.readTree(resp);

            return root.path("candidates").path(0)
                    .path("content").path("parts").path(0)
                    .path("text")
                    .asText("");

        } catch (WebClientResponseException e) {

            log.error("상태코드 = {}", e.getStatusCode());
            log.error("응답본문 = {}", e.getResponseBodyAsString());

            throw new RuntimeException(
                    "AI 응답 생성 실패: " + e.getResponseBodyAsString(),
                    e
            );

        } catch (Exception e) {

            log.error("Gemini 응답 파싱 실패", e);

            throw new RuntimeException(
                    "Gemini 응답 파싱 실패",
                    e
            );
        }
    }
}