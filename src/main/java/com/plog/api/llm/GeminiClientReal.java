package com.plog.api.llm;

import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.plog.api.common.exception.BadRequestException;
import com.plog.api.common.exception.TooManyRequestsException;
import com.plog.api.domain.aiguide.Persona;
import com.plog.api.pipeline.dto.BatchQuestion;
import com.plog.api.pipeline.dto.BatchQuestionsResponse;
import com.plog.api.pipeline.dto.ChatResponse;
import com.plog.api.pipeline.dto.ChatTurn;
import com.plog.api.pipeline.dto.ImagePart;
import com.plog.api.pipeline.dto.VisionResult;

import java.util.ArrayList;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "plog.gemini", name = "use-mock", havingValue = "false")
public class GeminiClientReal implements GeminiClient {

    private static final String PROMPT = """
        다음 사진을 분석해서 일기 작성에 도움이 될 정보를 JSON으로만 반환해 주세요.
        반드시 아래 키만 사용하고 JSON 외 다른 텍스트는 출력하지 마세요.

        톤 가이드 (매우 중요):
        - 일상 대화체로 담백하게 쓰세요. 20대가 친구에게 말하듯이.
        - "따스한 햇살 아래", "여유를 만끽하고", "~의 한 컷" 같은 문학적·시적 수사를 절대 쓰지 마세요.
        - "~입니다", "~풍경입니다" 같은 격식체 종결은 피하고 사실 위주로 짧게.
        - 형용사 남용 금지. 핵심 사실(누가/어디서/무엇을)만 담아 1줄.

        {
          "objects": ["사진의 주요 사물 3~6개, 일상 어휘 (예: 커피잔, 노트북)"],
          "scene": "어디서 뭘 하는지 한 줄 (예: 카페 야외 테이블에서 커피 마시는 중)",
          "mood": "분위기 형용사 1~2개 (예: 조용함, 들뜸)",
          "time_of_day": "새벽/아침/오전/낮/오후/저녁/밤/심야 중 하나",
          "weather_hint": "맑음/흐림/비/눈/안개 중 하나, 모르면 '미상'",
          "suggested_emotion": "사용자가 느꼈을 법한 감정 한 단어 (예: 편안함, 설렘)",
          "one_line_summary": "사진을 사실 그대로 한 줄 요약. 시적 표현 금지. (예: '벚꽃이 핀 공원에 사람들이 많이 나와 있다.')"
        }
        """;

    private static final Pattern JSON_BLOCK = Pattern.compile("\\{.*\\}", Pattern.DOTALL);

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final GeminiKeyManager keyManager;

    @Value("${plog.gemini.endpoint:https://generativelanguage.googleapis.com/v1beta}")
    private String endpoint;

    @Value("${plog.gemini.model:gemini-1.5-flash}")
    private String model;

    @Override
    public VisionResult analyzeImage(byte[] imageBytes, String mimeType) {
        if (keyManager.size() == 0) {
            throw new BadRequestException("Gemini api-keys 미설정 (plog.gemini.api-keys)");
        }
        String base64 = Base64.getEncoder().encodeToString(imageBytes);

        Map<String, Object> body = Map.of(
            "contents", List.of(Map.of(
                "parts", List.of(
                    Map.of("text", PROMPT),
                    Map.of("inline_data", Map.of(
                        "mime_type", mimeType,
                        "data", base64
                    ))
                )
            )),
            "generationConfig", Map.of(
                "temperature", 0.4,
                "topP", 0.9,
                "maxOutputTokens", 4096,
                "responseMimeType", "application/json",
                "thinkingConfig", Map.of("thinkingBudget", 0)
            )
        );

        long t0 = System.currentTimeMillis();
        String resp = postWithRotation(body, "analyzeImage");
        log.info("Gemini analyzeImage latency={}ms", System.currentTimeMillis() - t0);

        try {
            JsonNode root = objectMapper.readTree(resp);
            String finishReason = root.path("candidates").path(0).path("finishReason").asText("");
            String text = root.path("candidates").path(0).path("content").path("parts").path(0).path("text").asText("");
            log.info("Gemini finishReason={} text.len={}", finishReason, text.length());
            log.debug("Gemini raw text: {}", text);
            String json = extractJson(text);
            return objectMapper.readValue(json, VisionResult.class);
        } catch (Exception e) {
            log.error("Gemini 응답 파싱 실패. raw={}", resp);
            throw new BadRequestException("Gemini 응답 파싱 실패: " + e.getMessage());
        }
    }

    private static final String CHAT_SYSTEM_BASE = """
        당신은 사용자의 일기 작성을 돕는 친근한 한국인 친구입니다.
        첨부된 여러 장의 사진을 전체적으로 보고, 시간 흐름·장소 연관성·이미지 간 인과를
        사고하여 사용자에게 의미 있는 질문을 합니다.

        [사진 정보]
        각 사진의 시간·위치·장면은 첫 user 메시지 본문에 캡션으로 함께 전달됩니다.
        사진들이 같은 날 다른 시간대라면 자연스럽게 흐름을 연결해 질문하세요.
        (예: "아침에 한라산 쪽에서 벚꽃 보시고 점심엔 협재 카페로 옮기신 것 같아요. 어떤 흐름이었어요?")

        [질문 원칙]
        - 단순 사실 나열형 질문 금지. "뭐 마셨어요?" "분위기 어땠어요?" 류는
          일기로 쓰기에 너무 빈약합니다.
        - 사진 속 순간의 의미·관계·결심·이동 동기·인상 깊었던 한 장면 등 일기에
          쓸 만한 깊이를 끌어내세요.
        - 캡션의 시간/위치를 자연스럽게 언급해서 사용자가 흐름을 떠올리도록 유도.
        - 사진이 여러 장이면 한 장씩 균등하게 다루세요. 한 장에만 매몰되지 마세요.
        - "~죠?", "~어요?", "~했어요?" 같은 자연스러운 대화체.
        - 시적·문학적 수사 금지. 일상 어휘.
        - 한 번에 한 가지만 짧게.
        - 발화는 1~2문장 정도로 짧게. 길게 늘이지 마세요.
        - 적절한 이모지 1~2개로 가독성을 높이세요 (예: 🌸 ☕ 🍽️ 😊 🌅 🌙 🍵).
          같은 이모지를 매번 반복하지 마세요. 이모지 과용 금지.
        - 시간 표현은 친숙하게 — "오후 2시 35분", "19시 8분" 같은 분/초 단위
          정확 시각 절대 금지. "아침 일찍", "점심 무렵", "오후", "저녁" 같은
          자연 표현만 사용.
        - 한 발화 안에서 전체 동선(아침-점심-저녁 + 모든 지명) 반복 인용 절대 금지.
          현재 질문이 다루는 1개 사진의 시간/장소만 언급. 다른 사진으로 주제가
          넘어갈 때만 다음 사진의 장소/시간 언급.

        [첫 4턴 안에 반드시 다음 2가지 정보 끌어내기 — 누락 시 실격]
        1. 누구와 함께였는지 (또는 혼자였는지)
        2. 이 자리를 찾게 된 이유

        첫 인사 또는 첫 후속 질문에서 1번을 자연스럽게 물어보고,
        그다음 후속 질문에서 2번을 물어보세요.
        사용자가 답변에서 자연스럽게 이 정보를 줬다면 다시 묻지 말고 넘어가세요.

        [Max 직전 발화 규칙]
        - 동적 marker에서 "현재까지 사용자 답변 수"가 "최소 답변 수 - 1"에 도달하면
          다음 발화는 마무리 멘트(질문형 X) + ready_for_draft=true로 보내세요.
        - "이제 일기로 정리해볼게요" 류 자연 마무리.

        [마무리 조건 — 매우 중요]
        - 사진 갯수에 비례한 최소 답변 수까지는 절대 ready_for_draft=true 보내지 마세요.
        - 충분히 디테일을 끌어낸 후에만 "이제 일기로 정리해볼게요" 류의 마무리 멘트로
          응답하고 ready_for_draft=true.
        - 사용자가 명시적으로 "이제 됐어/충분해/그만" 류로 말하면 즉시 ready_for_draft=true.

        [응답 형식]
        반드시 JSON으로만, 다른 텍스트 없이:
        {"text": "다음에 할 말", "ready_for_draft": false}
        """;

    @Override
    public ChatResponse continueConversation(List<ImagePart> images, List<ChatTurn> history,
                                              int userTurnCount, int requiredMinTurns) {
        if (keyManager.size() == 0) {
            throw new BadRequestException("Gemini api-keys 미설정");
        }

        // 동적 marker — 매 호출마다 prompt 안에 사진 수·최소 답변 수 명시
        // 최대 턴 직전에 마무리 모드 진입을 AI에게 명시
        boolean isLastTurn = userTurnCount >= requiredMinTurns - 1;
        String dynamicMarker = String.format(
            "%n[이번 세션 컨텍스트]%n- 첨부된 사진 수: %d장%n- ready_for_draft=true 보내도 되는 최소 사용자 답변 수: %d회%n- 현재까지 사용자 답변: %d회%n- 마무리 모드: %s",
            images == null ? 0 : images.size(), requiredMinTurns, userTurnCount,
            isLastTurn ? "활성 — 다음 발화는 반드시 마무리 멘트(질문형 X) + ready_for_draft=true" : "비활성");
        String systemBlock = CHAT_SYSTEM_BASE + dynamicMarker;

        // 첫 user turn에 사진 N장 + 캡션 목록 + system 통합
        List<Map<String, Object>> contents = new ArrayList<>();
        boolean firstUserSent = false;
        for (ChatTurn t : history) {
            List<Map<String, Object>> parts = new ArrayList<>();
            if ("user".equals(t.role()) && !firstUserSent && images != null && !images.isEmpty()) {
                parts.add(Map.of("text", systemBlock + "\n\n" + buildImageCaptions(images) + "\n\n[사용자]: " + t.content()));
                for (ImagePart ip : images) {
                    parts.add(Map.of("inline_data", Map.of(
                        "mime_type", ip.mimeType(),
                        "data", Base64.getEncoder().encodeToString(ip.bytes())
                    )));
                }
                firstUserSent = true;
            } else {
                parts.add(Map.of("text", t.content()));
            }
            contents.add(Map.of("role", t.role(), "parts", parts));
        }

        // 사용자가 처음 진입한 경우(history 비어있음) — 가짜 첫 user 발화로 system + 모든 이미지 전달
        if (contents.isEmpty() && images != null && !images.isEmpty()) {
            List<Map<String, Object>> parts = new ArrayList<>();
            parts.add(Map.of("text", systemBlock + "\n\n" + buildImageCaptions(images)
                    + "\n\n[지시] 위 사진들을 보고 첫 인사 + 시간 흐름·장소 연관성을 언급하는 첫 질문을 짧게 해주세요."));
            for (ImagePart ip : images) {
                parts.add(Map.of("inline_data", Map.of(
                    "mime_type", ip.mimeType(),
                    "data", Base64.getEncoder().encodeToString(ip.bytes())
                )));
            }
            contents.add(Map.of("role", "user", "parts", parts));
        }

        Map<String, Object> body = Map.of(
            "contents", contents,
            "generationConfig", Map.of(
                "temperature", 0.7,
                "topP", 0.9,
                "maxOutputTokens", 1024,
                "responseMimeType", "application/json",
                "thinkingConfig", Map.of("thinkingBudget", 0)
            )
        );

        long t0 = System.currentTimeMillis();
        String resp;
        try {
            resp = postWithRotation(body, "continueConversation");
        } catch (TooManyRequestsException e) {
            // 모든 키 quota 소진 시 결정론 fallback
            log.warn("Gemini 모든 키 quota → CONVERSATION 결정론 fallback");
            return chatFallback(userTurnCount, requiredMinTurns);
        } catch (Exception e) {
            // 서버 일시 장애로 모든 키 실패 시에도 결정론 fallback으로 응답 보장
            log.warn("Gemini 호출 실패 ({}) → CONVERSATION 결정론 fallback", e.getMessage());
            return chatFallback(userTurnCount, requiredMinTurns);
        }
        log.info("Gemini chat latency={}ms userTurns={} requiredMin={} images={}",
                System.currentTimeMillis() - t0, userTurnCount, requiredMinTurns,
                images == null ? 0 : images.size());

        try {
            JsonNode root = objectMapper.readTree(resp);
            String text = root.path("candidates").path(0).path("content").path("parts").path(0).path("text").asText("");
            String json = extractJson(text);
            ChatResponse parsed = objectMapper.readValue(json, ChatResponse.class);
            // 서버 사이드 안전망 — Gemini가 prompt 무시하고 일찍 ready_for_draft=true 보내도 차단
            if (parsed.readyForDraft() && userTurnCount < requiredMinTurns) {
                log.warn("Gemini ready_for_draft=true 너무 일찍(turn={} < min={}) — 서버에서 false로 보정",
                        userTurnCount, requiredMinTurns);
                return ChatResponse.builder().text(parsed.text()).readyForDraft(false).build();
            }
            // 반대 보정 — requiredMinTurns 이상 도달 시 Gemini가 false 보내도 강제 true
            // (Gemini의 보수적 판단으로 사용자가 무한 대화하는 것 방지)
            if (!parsed.readyForDraft() && userTurnCount >= requiredMinTurns) {
                log.warn("Gemini ready_for_draft=false (turn={} >= min={}) — 서버에서 true로 보정",
                        userTurnCount, requiredMinTurns);
                return ChatResponse.builder().text(parsed.text()).readyForDraft(true).build();
            }
            return parsed;
        } catch (Exception e) {
            log.error("Gemini chat 응답 파싱 실패. raw={}", resp);
            throw new BadRequestException("Gemini chat 응답 파싱 실패: " + e.getMessage());
        }
    }

    /** quota 초과 시 CONVERSATION 결정론 fallback. */
    private static ChatResponse chatFallback(int userTurnCount, int requiredMinTurns) {
        String[] fallbackQuestions = {
            "🌸 사진 잘 봤어요. 사진을 찍은 순간 누구랑 함께였어요?",
            "☕ 좋은 시간이었겠네요. 가장 기억에 남는 한 장면이 있다면 뭐예요?",
            "🍽️ 사진을 찍을 때 기분이 어땠어요? 짧게 표현해 본다면?",
            "🌅 같은 자리에 다시 가게 된다면 또 가고 싶은 이유가 있을까요?",
            "😊 사진 속 시간을 한 줄로 정리한다면 어떤 문장이 떠올라요?"
        };
        String final_ = "잘 들었어요! ✨ 이제 일기로 정리해볼게요.";
        if (userTurnCount >= requiredMinTurns) {
            return ChatResponse.builder().text(final_).readyForDraft(true).build();
        }
        int idx = Math.min(userTurnCount, fallbackQuestions.length - 1);
        return ChatResponse.builder().text(fallbackQuestions[idx]).readyForDraft(false).build();
    }

    /** 사진별 시간/위치/장면 캡션을 "사진N: ..." 형태로 나열. */
    private static String buildImageCaptions(List<ImagePart> images) {
        StringBuilder sb = new StringBuilder("[첨부된 사진 정보]\n");
        for (int i = 0; i < images.size(); i++) {
            String cap = images.get(i).caption();
            sb.append("사진").append(i + 1).append(": ")
              .append(cap == null || cap.isBlank() ? "(시간/위치 정보 없음)" : cap.trim())
              .append('\n');
        }
        return sb.toString();
    }

    @Override
    public String generateDraft(String personaSystemPrompt, String userDiaryGuideMd, String contextDescription) {
        if (keyManager.size() == 0) {
            throw new BadRequestException("Gemini api-keys 미설정");
        }

        StringBuilder system = new StringBuilder();
        system.append("당신은 사용자를 대신해 한국어 일기 초안을 작성합니다.\n");
        system.append("180~400자, 1편의 자연스러운 일기. 사용자가 직접 쓴 것처럼 1인칭 시점.\n\n");

        system.append("[작성 원칙 — 매우 중요, 모두 반드시 지킬 것]\n");
        system.append("1. 1인칭(\"나\"의 시점)으로 작성. 사진 묘사를 그대로 옮기지 마세요.\n");
        system.append("   - 나쁜 예: \"카페 야외 테이블에 커피와 디저트가 놓여 있고, 옆에 가방이 보인다.\"\n");
        system.append("   - 좋은 예: \"오늘은 친구랑 그 카페에 갔다. 야외 자리에 앉아 디저트랑 커피를 시켰다.\"\n");
        system.append("2. 사용자 답변/대화의 반말 종결(\"~어\", \"~다더라\", \"~았어\")을 일기체(\"~었다\", \"~다고 했다\")로 자연 변환.\n");
        system.append("   - 나쁜 예: \"돌아보면 친구랑 갔어. 그리고 오랜만에 봐서 수다 많이 떨었어.\"\n");
        system.append("   - 좋은 예: \"오랜만에 만난 친구랑 한참 수다를 떨었다.\"\n");
        system.append("3. \"돌아보면\", \"그리고\" 같은 단순 접속어로 답변을 그대로 이어붙이지 마세요.\n");
        system.append("   → 시간 흐름·인과·감정 연결로 자연스럽게 풀어내세요.\n");
        system.append("4. 마무리 정형구 금지. 다음 표현 절대 사용 금지:\n");
        system.append("   - \"오늘의 감정을 한 단어로 적자면, '~'이다.\"\n");
        system.append("   - \"이런 하루를 기록해 두고 싶었다.\"\n");
        system.append("   → 사진 속 한 장면, 짧은 다짐, 감정 잔상으로 자연스럽게 끝내세요. 매번 다른 표현으로.\n");
        system.append("5. 시적·문학적 수사 자제. \"따스한 햇살\", \"여유를 만끽\", \"~의 한 컷\" 같은 클리셰 금지.\n");
        system.append("6. 사용자가 실제로 한 말·디테일은 최대한 살리되, 부풀리거나 없던 내용을 지어내지 마세요.\n");
        system.append("7. 사진이 여러 장일 때는 모든 사진의 디테일을 한 편의 일기에 자연스럽게 녹이세요.\n");
        system.append("   - 시간 흐름이 있으면 그 흐름대로 일기를 전개. 한 장면만 부각하고 나머지를 무시하지 마세요.\n");
        system.append("   - 시간/위치 정보가 주어졌다면 일기에 자연스럽게 녹여 하루의 동선이 드러나게.\n");
        system.append("8. 본문은 2~3개 문단으로 나누어 \\n\\n으로 구분.\n");
        system.append("   - 도입(상황) / 본론(전개·디테일) / 마무리(감정·여운) 흐름 권장.\n");
        system.append("   - 한 문단은 2~5문장.\n");
        system.append("9. 일기 마무리에 어울리는 이모지 1~2개를 자연스러운 위치에 넣으세요.\n");
        system.append("   - 예: 🌸 ☕ 🍽️ 😊 🌅 🌙 🍵 ✨ 🚶 📷 등 그 날 분위기·장면에 맞는 것.\n");
        system.append("   - 한 일기에 같은 이모지 반복 금지. 본문 첫 줄에는 넣지 마세요.\n");
        system.append("   - 페르소나가 FORMAL일 땐 이모지 사용 자제.\n");
        system.append("10. 모호한 지시어 절대 금지 — 출력에 다음 단어가 한 번이라도 등장하면 실격:\n");
        system.append("    \"그날\", \"그곳\", \"그 시간\", \"그 자리\", \"그 순간\", \"그때\".\n");
        system.append("    - 시간은 \"아침\", \"점심 무렵\", \"오후\", \"저녁\" 같은 친숙한 표현으로.\n");
        system.append("    - 장소는 캡션의 지명을 그대로 사용 (\"한라산\", \"협재\", \"서귀포\" 등).\n");
        system.append("    - 캡션에 정보가 없을 때만 \"오늘 들른 카페에서\", \"사진 속 자리에서\" 정도로 표현.\n");
        system.append("    - 굳이 시간/장소를 가리킬 필요가 없으면 그 부분을 생략하세요.\n");
        system.append("11. 시간 표현은 반드시 친숙하게 — \"오후 2시 35분\", \"19시 8분\" 같은 분/초 단위 정확 시각 절대 금지.\n");
        system.append("    \"아침 일찍\", \"점심 무렵\", \"오후\", \"저녁 즈음\" 같은 자연스러운 한국어 표현만 사용.\n");
        system.append("11-2. 일기 본문에 같은 사진/장소를 두 번 이상 반복 언급 금지.\n");
        system.append("    각 사진은 일기 본문에 한 번만 등장 — 시간 흐름대로 자연스럽게 이어가세요.\n");
        system.append("    중복 인용 금지: '한라산에서 ~ 했다. 한라산이 좋았다' 같은 같은 장소 반복 금지.\n");
        system.append("12. 일기에 반드시 포함할 정보:\n");
        system.append("    (a) 누구와 함께였는지 — 사용자 답변 그대로 또는 자연 변환.\n");
        system.append("    (b) 오늘 이 자리/장소를 찾은 이유 — 사용자 답변 그대로 또는 자연 변환.\n");
        system.append("    (c) 사용자가 \"가장 인상 깊었던 한 가지\"로 답한 디테일 — 일기 본론에 강조.\n");
        system.append("    이 셋 중 어느 것도 사용자 답변에 없으면 그 항목은 생략. 답변에 있으면 반드시 포함.\n\n");

        system.append("[페르소나 톤]\n").append(personaSystemPrompt == null ? "" : personaSystemPrompt).append("\n\n");
        if (userDiaryGuideMd != null && !userDiaryGuideMd.isBlank()) {
            system.append("[사용자 개인 가이드 (반드시 반영)]\n").append(userDiaryGuideMd).append("\n\n");
        }
        system.append("[출력 형식]\n반드시 JSON: {\"draft\": \"한국어 일기 본문\"}\n");

        Map<String, Object> body = Map.of(
            "contents", List.of(Map.of(
                "role", "user",
                "parts", List.of(
                    Map.of("text", system.toString() + "\n\n[작성에 쓸 정보]\n" + contextDescription)
                )
            )),
            "generationConfig", Map.of(
                "temperature", 0.6,
                "topP", 0.9,
                "maxOutputTokens", 2048,
                "responseMimeType", "application/json",
                "thinkingConfig", Map.of("thinkingBudget", 0)
            )
        );

        String resp = postWithRotation(body, "generateDraft");
        try {
            JsonNode root = objectMapper.readTree(resp);
            String text = root.path("candidates").path(0).path("content").path("parts").path(0).path("text").asText("");
            String json = extractJson(text);
            JsonNode draftNode = objectMapper.readTree(json);
            String draft = draftNode.path("draft").asText("");
            if (draft.isBlank()) throw new BadRequestException("Gemini draft 응답 비어있음");
            return draft;
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            log.error("Gemini generateDraft 파싱 실패: {}", e.getMessage());
            throw new BadRequestException("Gemini 일기 응답 파싱 실패: " + e.getMessage());
        }
    }

    @Override
    public String refineDiaryGuide(String existingMd, String newFeedback, int satisfactionScore, int maxLines) {
        if (keyManager.size() == 0) {
            throw new BadRequestException("Gemini api-keys 미설정");
        }

        String prompt = String.format("""
            아래는 한 사용자의 일기 작성 취향을 정리한 가이드 MD와 새로 학습할 취향입니다.
            이 둘을 종합해서 %d줄 이내의 한국어 마크다운 가이드로 압축·재정리해 주세요.
            이 가이드는 다음부터 그 사용자의 일기 초안을 작성할 때 system prompt로 자동 주입됩니다.

            규칙:
            - 첫 줄은 "# 일기 작성 취향" 헤더 1줄.
            - 나머지는 "- " 불릿 항목 (각 항목 1줄, 핵심만).
            - 중복·유사 항목 통합.
            - 새 학습 내용이 기존과 모순되면 새 내용 우선.
            - 총 %d줄 절대 초과 금지.
            - 출력은 마크다운 그대로 (코드블록 ``` 감싸지 마세요).

            [기존 가이드 MD]
            %s

            [새로 학습할 작성 취향]
            %s
            """, maxLines, maxLines,
            existingMd == null || existingMd.isBlank() ? "(없음)" : existingMd,
            newFeedback);

        Map<String, Object> body = Map.of(
            "contents", List.of(Map.of("role", "user", "parts", List.of(Map.of("text", prompt)))),
            "generationConfig", Map.of(
                "temperature", 0.3,
                "maxOutputTokens", 1024,
                "thinkingConfig", Map.of("thinkingBudget", 0)
            )
        );

        String resp = postWithRotation(body, "refineDiaryGuide");
        try {
            JsonNode root = objectMapper.readTree(resp);
            return root.path("candidates").path(0).path("content").path("parts").path(0).path("text").asText("");
        } catch (Exception e) {
            log.error("Gemini refineDiaryGuide 파싱 실패: {}", e.getMessage());
            throw new BadRequestException("Gemini 가이드 응답 파싱 실패: " + e.getMessage());
        }
    }

    @Override
    public BatchQuestionsResponse generateBatchQuestions(List<ImagePart> images, Persona persona, int questionCount) {
        if (keyManager.size() == 0) {
            throw new BadRequestException("Gemini api-keys 미설정");
        }

        Persona p = persona == null ? Persona.DEFAULT : persona;
        StringBuilder prompt = new StringBuilder();
        prompt.append("당신은 사용자의 일기 작성을 돕는 친근한 한국인 친구입니다.\n");
        prompt.append("첨부된 ").append(images == null ? 0 : images.size())
              .append("장의 사진(아래 캡션에 시간/위치/장면이 포함)을 전체적으로 보고,\n");
        prompt.append("사용자가 일기를 풍성하게 쓸 수 있도록 깊이 있는 질문 ")
              .append(questionCount).append("개를 한국어로 만들어주세요.\n");
        prompt.append("각 질문에 사용자가 빠르게 고를 수 있는 자연스러운 답변 후보 3개도 함께 제안해주세요.\n\n");

        prompt.append("[질문 단계 구조 — 반드시 이 순서로 ").append(questionCount).append("개 생성]\n");
        prompt.append("Q1 (필수): 누구와 함께였나요? (또는 혼자였다면 어떻게 왔는지)\n");
        prompt.append("    후보: \"가족과 함께 다녀왔다\", \"친구와 같이 갔다\", \"혼자 잠시 다녀왔다\", \"연인과 함께였다\" 같이.\n");
        prompt.append("Q2 (필수): 오늘 이 장소를 찾은 이유가 있었나요?\n");
        prompt.append("    후보: \"가족과 시간을 보내려고\", \"집 근처라서 가볍게 들렀다\", \"전부터 한 번 와보고 싶었다\", \"혼자 마음을 정리하고 싶었다\" 같이.\n");
        prompt.append("Q3 ~ Q").append(Math.max(3, questionCount - 2)).append(": 사진별 시간 흐름 deep dive. 각 사진의 장면·디테일에 집중.\n");
        prompt.append("Q").append(questionCount - 1).append(" (deep dive 핵심): Q3~Q").append(Math.max(3, questionCount - 2))
              .append("에서 사용자가 가장 인상 깊다고 답한 한 장면을 더 깊이 파고드는 질문.\n");
        prompt.append("    예) \"그 순간의 표정/대화/생각/소리/향이 있다면 어떤 거였어요?\", \"그 한 장면을 다시 떠올리면 어떤 감정이 가장 먼저 올라와요?\"\n");
        prompt.append("Q").append(questionCount).append(" (의미·여운): 그 한 장면이 자신에게 어떤 의미로 남았는지, ");
        prompt.append("다음에도 비슷한 자리를 찾고 싶은지, 누군가에게 들려주고 싶은지 등 미래·여운 질문.\n\n");

        prompt.append("[질문 원칙 — 매우 중요]\n");
        prompt.append("1. 모호 지시어 절대 금지 — 출력에 다음 단어가 한 번이라도 등장하면 실격:\n");
        prompt.append("   \"그날\", \"그곳\", \"그 시간\", \"그 자리\", \"그 순간\", \"그때\".\n");
        prompt.append("   - 시간 정보(EXIF)가 있으면 \"아침에\", \"점심 무렵\", \"저녁에\"처럼 명시적으로.\n");
        prompt.append("   - 위치 정보가 있으면 \"한라산에서\", \"협재 카페에서\", \"서귀포에서\"처럼 지명을 그대로 사용.\n");
        prompt.append("   - 알 수 없을 때만 \"사진 속 자리에서\", \"사진을 보니\" 정도로 자연스럽게 회피.\n");
        prompt.append("2. 단순 사실 나열형·빈약 질문 금지:\n");
        prompt.append("   - 나쁜 예: \"한 단어로 표현하면?\", \"분위기 어땠어요?\", \"뭐 마셨어요?\"\n");
        prompt.append("   - 좋은 예: \"협재 카페에서 가장 인상 깊었던 메뉴 하나만 떠올려본다면 어떤 거예요?\"\n");
        prompt.append("3. 사진의 디테일(사물·장면·분위기)을 한두 개 인용해서 사용자가 사진 속 순간을 떠올릴 단서를 주세요.\n");
        prompt.append("4. 사진 속 순간의 의미·관계·이동 동기·인상 깊었던 한 장면을 끌어내는 방향.\n");
        prompt.append("4-2. 시간 표현은 친숙하게 — \"오후 2시 35분\", \"19시 8분\" 같은 분/초 단위 정확 시각 금지. \"아침 일찍\", \"점심 무렵\", \"오후\", \"저녁\" 같은 자연 표현만 사용.\n");
        prompt.append("4-3. 한 질문 안에서 전체 동선(아침-점심-저녁 + 모든 지명) 반복 인용 절대 금지.\n");
        prompt.append("    각 질문은 그 질문이 다루는 1개 사진의 시간/장소만 언급하세요.\n");
        prompt.append("    예) Q3가 점심 카페에 대한 질문이면 \"협재 카페에서...\"만. \"아침엔 한라산...\" 추가 금지.\n");
        prompt.append("5. 사진이 여러 장이면 한 장에만 매몰되지 말고 균등하게 다루세요.\n");
        prompt.append("   시간 흐름이 보이면 흐름대로 분포 (아침 → 점심 → 저녁).\n");
        prompt.append("6. 질문 유형은 EMOTION(감정) / SITUATION(상황) / MEANING(의미) 중 하나로 분류.\n");
        prompt.append("7. 페르소나 톤: ").append(p.getSystemPromptFragment()).append("\n\n");

        prompt.append("[답변 후보 원칙]\n");
        prompt.append("- 각 후보는 1~2문장. 일기에 그대로 옮겨도 자연스러운 한국어 문장.\n");
        prompt.append("- 후보 3개는 서로 다른 방향성으로 (예: 담백한 사실 / 감정 한 줄 / 특별한 디테일).\n");
        prompt.append("- 후보에도 \"그날/그곳\" 같은 모호 표현 금지. 시간/장소를 살린 구체적 표현 권장.\n");
        prompt.append("- 클리셰('따스한 햇살', '평온한 한때', '소중한 시간') 금지.\n\n");

        prompt.append("[사진 정보]\n").append(buildImageCaptions(images)).append('\n');

        prompt.append("[출력 형식]\n");
        prompt.append("반드시 JSON만, 다른 텍스트 없이:\n");
        prompt.append("{\"questions\": [{\"text\": \"...\", \"type\": \"SITUATION\", \"suggested_answers\": [\"...\", \"...\", \"...\"]}, ...]}\n");

        List<Map<String, Object>> parts = new ArrayList<>();
        parts.add(Map.of("text", prompt.toString()));
        if (images != null) {
            for (ImagePart ip : images) {
                parts.add(Map.of("inline_data", Map.of(
                        "mime_type", ip.mimeType(),
                        "data", Base64.getEncoder().encodeToString(ip.bytes())
                )));
            }
        }

        Map<String, Object> body = Map.of(
            "contents", List.of(Map.of("role", "user", "parts", parts)),
            "generationConfig", Map.of(
                "temperature", 0.7,
                "topP", 0.9,
                "maxOutputTokens", 4096,
                "responseMimeType", "application/json",
                "thinkingConfig", Map.of("thinkingBudget", 0)
            )
        );

        long t0 = System.currentTimeMillis();
        String resp = postWithRotation(body, "generateBatchQuestions");
        log.info("Gemini batch questions latency={}ms count={} images={}",
                System.currentTimeMillis() - t0, questionCount,
                images == null ? 0 : images.size());

        try {
            JsonNode root = objectMapper.readTree(resp);
            String text = root.path("candidates").path(0).path("content").path("parts").path(0).path("text").asText("");
            String json = extractJson(text);
            return objectMapper.readValue(json, BatchQuestionsResponse.class);
        } catch (Exception e) {
            log.error("Gemini batch questions 파싱 실패. raw={}", resp);
            throw new BadRequestException("Gemini batch questions 응답 파싱 실패: " + e.getMessage());
        }
    }

    /**
     * Gemini POST + 키 자동 회전. 429 발생 시 다음 키로 회전 후 재시도.
     * 모든 키 quota 소진 시 TooManyRequestsException.
     */
    private String postWithRotation(java.util.Map<String, Object> body, String operation) {
        int total = Math.max(1, keyManager.size());
        Exception last = null;
        for (int attempt = 0; attempt < total; attempt++) {
            String key = keyManager.current();
            int currentIdx = keyManager.currentIndex();
            String url = endpoint + "/models/" + model + ":generateContent?key=" + key;
            try {
                String resp = webClient.post()
                        .uri(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(body)
                        .retrieve()
                        .bodyToMono(String.class)
                        .block();
                if (currentIdx != 0 || attempt > 0) {
                    log.info("{} 성공 (키 idx={}, attempt={})", operation, currentIdx, attempt);
                }
                return resp;
            } catch (Exception e) {
                last = e;
                // quota(429) + 일시 장애(503/502/504) 발생 시 다음 키로 회전
                if (isRetriableError(e)) {
                    log.warn("{} 키 idx={} retriable 에러 ({}) → 다음 키로 회전 (남은 시도 {})",
                            operation, currentIdx, e.getMessage().substring(0, Math.min(80, e.getMessage().length())),
                            total - attempt - 1);
                    keyManager.rotate();
                    continue;
                }
                log.error("{} 비재시도 에러: {}", operation, e.getMessage());
                throw new BadRequestException(operation + " 호출 실패: " + e.getMessage());
            }
        }
        throw new TooManyRequestsException(
            "모든 Gemini 키 (" + total + "개) quota 소진. 잠시 후 재시도하세요. "
            + "마지막 에러: " + (last == null ? "(none)" : last.getMessage()));
    }

    private String extractJson(String text) {
        if (text == null) return "{}";
        Matcher m = JSON_BLOCK.matcher(text);
        if (m.find()) return m.group();
        return text;
    }

    private boolean isQuotaError(Throwable e) {
        String msg = e.getMessage() == null ? "" : e.getMessage();
        return msg.contains("429") || msg.contains("Too Many Requests") || msg.contains("quota");
    }

    /** 재시도 가능 에러 판별 — quota(429) + 일시 장애(503/502/504). */
    private boolean isRetriableError(Throwable e) {
        if (isQuotaError(e)) return true;
        String msg = e.getMessage() == null ? "" : e.getMessage();
        return msg.contains("503") || msg.contains("Service Unavailable")
            || msg.contains("502") || msg.contains("Bad Gateway")
            || msg.contains("504") || msg.contains("Gateway Timeout")
            || msg.contains("UNAVAILABLE");
    }
}
