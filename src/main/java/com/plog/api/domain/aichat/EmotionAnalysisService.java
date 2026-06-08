package com.plog.api.domain.aichat;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmotionAnalysisService {

    private final AiChatGeminiClient geminiClient;

    public EmotionResult analyze(List<AiChatMessage> messages) {
        // 대화 내용 합치기
        String conversation = messages.stream()
            .map(m -> m.getSender() + ": " + m.getMessage())
            .reduce("", (a, b) -> a + "\n" + b);

        String prompt = """
            아래 대화를 분석해서 JSON으로만 응답해. 다른 말 하지 마. 마크다운 쓰지 마.
            {
              "emotion": "기쁨/슬픔/불안/분노/평온/혼란/설렘 중 하나",
              "score": 0.0~1.0 사이 숫자,
              "summary": "한 줄 요약"
            }
            
            대화:
            """ + conversation;

        try {
            String response = geminiClient.chat("", List.of(), prompt);

            // 혹시 ```json ``` 감싸져 있으면 제거
            String cleaned = response
                .replaceAll("```json", "")
                .replaceAll("```", "")
                .trim();

            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(cleaned, EmotionResult.class);

        } catch (Exception e) {
            log.warn("감정 분석 실패: {}", e.getMessage());
            return new EmotionResult("알 수 없음", 0.5f, "");
        }
    }
}