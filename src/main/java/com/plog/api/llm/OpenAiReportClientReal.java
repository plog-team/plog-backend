package com.plog.api.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.plog.api.common.exception.BadRequestException;
import com.plog.api.pipeline.dto.DiaryAnalysisResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "plog.gemini", name = "use-mock", havingValue = "false")
public class OpenAiReportClientReal implements OpenAiReportClient {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    @Value("${plog.openai.api-key:}")
    private String apiKey;

    @Value("${plog.openai.model:gpt-4o-mini}")
    private String model;

    @Value("${plog.openai.endpoint:https://api.openai.com/v1}")
    private String endpoint;

    @Override
    public DiaryAnalysisResult analyzeEmotionFromDiary(String body, LocalDate date) {
        String system = """
            당신은 개인 성찰을 돕는 일기 감정 분석 전문가입니다.
            사용자의 한국어 일기에서 감정을 정확하게 추출하는 것이 목적입니다.
            판단 없이 일기에 담긴 감정을 있는 그대로 분석하세요.
            반드시 JSON으로만 응답하세요.""";
        String user = String.format("""
            아래 일기에서 오늘 느낀 감정을 분석해주세요. 복수의 감정이 있으면 모두 추출합니다.

            날짜: %s
            일기 내용: %s

            감정이 명확한 경우 (단일):   {"emotions": ["평온"], "needsClarification": false, "question": null, "options": null}
            감정이 명확한 경우 (복수):   {"emotions": ["기쁨", "설렘"], "needsClarification": false, "question": null, "options": null}
            감정이 모호하거나 파악 불가: {"emotions": null, "needsClarification": true, "question": "이 날 가장 가까운 감정을 골라주세요.", "options": ["슬픔", "우울", "%s"]}

            규칙:
            - 날짜는 참고용이며 반드시 일기 본문에서만 감정을 추론하세요 (날짜 자체로 감정을 추론하지 말 것)
            - 감정이 명확하면 일기에서 느껴지는 감정을 모두 emotions 배열에 포함
            - 감정 단어는 일기에서 직접 느껴지는 감정을 가장 잘 표현하는 한국어 단어로 자유롭게 선택
            - 감정 단어는 명사형 한 단어 (예: "억울함", "홀가분함", "설렘" 등 — 예시에 얽매이지 말 것)
            - options 마지막 항목은 항상 "%s"로 고정
            - 나머지 options는 반드시 일기 내용에서 추론한 구체적인 감정 단어 2개. "선택지1" 같은 형식 절대 금지.
            - 모호 판단 기준: 감정 표현이 전혀 없음, 오타로 인해 해석 불가
            """, date, body == null ? "" : body, EMOTION_SKIP_ANSWER, EMOTION_SKIP_ANSWER);

        String resp = callJsonApi(system, user, "analyzeEmotionFromDiary", 0.3);
        try {
            return objectMapper.readValue(resp, DiaryAnalysisResult.class);
        } catch (Exception e) {
            log.error("analyzeEmotionFromDiary 파싱 실패: {}", e.getMessage());
            return new DiaryAnalysisResult(null, null, false, null, null);
        }
    }

    @Override
    public DiaryAnalysisResult analyzePlaceFromDiary(String body, String location, LocalDate date) {
        String locationHint = (location != null && !location.isBlank()) ? location : "(없음)";
        String system = """
            당신은 개인 성찰을 돕는 일기 장소 분석 전문가입니다.
            사용자의 한국어 일기에서 방문 장소와 그 장소에서 느낀 감정을 정확하게 추출하는 것이 목적입니다.
            반드시 JSON으로만 응답하세요.""";
        String user = String.format("""
            아래 일기에서 오늘 방문한 주요 장소와, 그 장소에서 느낀 감정을 분석해주세요.

            날짜: %s
            참고 장소 필드(선택적 힌트): %s
            일기 내용: %s

            ━━━ [장소 판단] 반드시 이 순서대로 적용 ━━━

            1단계 — 이동 없음 → places=null, needsClarification=false
            조건: 외출·방문·이동 내용이 전혀 없음 (집에서 통화, 독서, 혼자 생각, 침대에 누워있기 등)

            2단계 — 장소 명확 → places=["장소명"], needsClarification=false
            조건: 아래 중 하나라도 해당하면 명확한 것으로 판단
              · 고유 장소명이 본문에 직접 언급됨 ("한강 공원", "홍대", "스타벅스" 등)
              · "집", "학교", "회사", "직장", "병원" 처럼 개인 기반 공간이 명확히 언급됨
              · 참고 장소 필드가 있고 본문 맥락과 자연스럽게 일치함
            규칙: 본문에 여러 장소가 나오면 가장 많이 언급되거나 핵심적인 곳 하나를 선택

            3단계 — 장소 모호 → places=null, needsClarification=true
            조건: 이동·방문이 있음은 명확하나 아래 중 하나에 해당하면 반드시 Clarification 요청
              · 지시대명사만 사용됨: "거기", "그곳", "그 카페", "그 식당", "그 집", "그 근처", "거기서" 등
              · 장소 카테고리(종류)는 언급됐으나 고유명이 없고 참고 장소 필드도 없음 ("어느 카페", "한 식당", "근처 공원")
              · 본문 전체에서 어떤 장소도 유추할 수 없는 경우
            중요: 지시대명사가 있으면 참고 장소 필드가 있어도 Clarification을 우선 요청할 것
              (참고 필드는 사용자가 나중에 보정용으로 입력한 것일 수 있음)

            ━━━ [감정 판단] 장소 판단 이후에만 적용 ━━━
            · places가 null이면 → emotions 반드시 null
            · places가 추출됐으면 → 그 장소에서 느낀 주된 감정을 본문에서 추론
            · 감정이 명확하면 → 일기 맥락에 가장 잘 맞는 한국어 명사형 감정 단어 하나를 자유롭게 선택
            · 감정이 불명확하거나 언급 없으면 → null
            · 감정 때문에 needsClarification=true로 만드는 것은 절대 금지

            ━━━ 응답 예시 (경우별) ━━━
            이동 없음:    {"places": null, "emotions": null, "needsClarification": false, "question": null, "options": null}
            단일 장소:    {"places": ["한강 공원"], "emotions": ["평온"], "needsClarification": false, "question": null, "options": null}
            복수 장소:    {"places": ["홍대", "강남"], "emotions": ["설렘"], "needsClarification": false, "question": null, "options": null}
            장소 모호:    {"places": null, "emotions": null, "needsClarification": true, "question": "이 날 방문한 장소가 어디인가요?", "options": ["카페", "공원", "기타 (직접 입력)"]}
            지시대명사:   {"places": null, "emotions": null, "needsClarification": true, "question": "이 날 방문한 장소가 어디인가요?", "options": ["본문 추론 후보A", "본문 추론 후보B", "기타 (직접 입력)"]}
            일부 모호:    {"places": ["홍대"], "emotions": ["기쁨"], "needsClarification": false, "question": null, "options": null}
            (위: "홍대에서 놀다가 거기서 저녁" → 명확한 홍대만 추출, 모호한 거기는 무시)

            ━━━ 공통 규칙 ━━━
            · places는 배열. 방문 장소가 없으면 null, 있으면 ["장소1", "장소2"] 형식
            · 명확한 장소와 모호한 장소가 섞이면 명확한 것만 places에 포함하고 needsClarification=false
            · places가 null이고 이동이 있으면 needsClarification=true
            · options는 본문에서 직접 언급되거나 맥락상 합리적으로 추론되는 장소만 포함
            · 본문에 힌트가 전혀 없으면 ["집", "직장/학교", "기타 (직접 입력)"] 사용
            · "장소1", "장소2" 같은 형식 절대 금지
            · options 마지막 항목은 항상 "기타 (직접 입력)"
            """, date, locationHint, body == null ? "" : body);

        String resp = callJsonApi(system, user, "analyzePlaceFromDiary", 0.3);
        try {
            return objectMapper.readValue(resp, DiaryAnalysisResult.class);
        } catch (Exception e) {
            log.error("analyzePlaceFromDiary 파싱 실패: {}", e.getMessage());
            List<String> fallback = (location != null && !location.isBlank()) ? List.of(location) : null;
            return new DiaryAnalysisResult(null, fallback, fallback == null,
                fallback == null ? "이 날 주요 장소가 어디였나요?" : null,
                fallback == null ? List.of("카페", "집", "직장") : null);
        }
    }

    @Override
    public String generateEmotionReportContent(String diariesSummaryJson, String guideMarkdown) {
        String guide = (guideMarkdown != null && !guideMarkdown.isBlank())
            ? "\n[사용자 맞춤 가이드]\n" + guideMarkdown + "\n" : "";
        String system = """
            당신은 개인 성찰을 돕는 감정 일기 분석가입니다.
            사용자의 주간 일기 데이터를 바탕으로 따뜻하고 통찰력 있는 감정 리포트를 작성합니다.
            반드시 JSON으로만 응답하세요.""";
        String user = String.format("""
            아래 주간 일기 데이터를 바탕으로 감정 리포트 내러티브를 작성해주세요.
            %s
            [일기 데이터]
            %s

            요구사항:
            - 200~400자 한국어 내러티브
            - 사용자에게 말하는 2인칭 시점 ("이번 주 당신은...")
            - 감정 레이블 나열 금지 ("기쁨을 느꼈습니다" 같은 서술 대신, excerpt에서 읽히는 실제 있었던 일과 연결해 감정을 표현)
            - 감정이 여러 개인 날은 감정의 복합성이나 변화를 자연스럽게 녹여낼 것
            - excerpt가 없는 날은 감정 흐름만 간략히 언급
            - 클리셰·정형 문구 금지

            반드시 JSON만: {"content": "..."}
            """, guide, diariesSummaryJson);

        String resp = callJsonApi(system, user, "generateEmotionReportContent", 0.55);
        try {
            JsonNode node = objectMapper.readTree(resp);
            return node.path("content").asText("이번 주의 감정 기록을 분석했습니다.");
        } catch (Exception e) {
            log.error("generateEmotionReportContent 파싱 실패: {}", e.getMessage());
            return "이번 주의 감정 기록을 분석했습니다.";
        }
    }

    @Override
    public String generatePlaceReportContent(String diariesSummaryJson, String guideMarkdown) {
        String guide = (guideMarkdown != null && !guideMarkdown.isBlank())
            ? "\n[사용자 맞춤 가이드]\n" + guideMarkdown + "\n" : "";
        String system = """
            당신은 개인 성찰을 돕는 장소 일기 분석가입니다.
            사용자의 월간 장소 방문 데이터를 바탕으로 라이프스타일을 통찰하는 리포트를 작성합니다.
            반드시 JSON으로만 응답하세요.""";
        String user = String.format("""
            아래 월간 일기 장소 데이터를 분석하여 리포트 내러티브를 작성해주세요.
            %s
            [일기 데이터]
            %s

            요구사항:
            - 200~400자 한국어 내러티브
            - 사용자에게 말하는 2인칭 시점 ("이번 달 당신은...")
            - 장소 방문 패턴에서 사용자의 생활 습관, 취향, 라이프스타일을 읽어내는 이야기를 써주세요
            - 자주 가는 장소가 그 사람에 대해 무엇을 말해주는지, 어떤 공간을 좋아하는 사람인지를 흥미롭게 표현하세요
            - 단순 방문 횟수 나열 금지 — "A를 4번, B를 2번" 식의 서술은 쓰지 마세요
            - 감정 데이터가 없는 장소는 감정에 대한 언급을 하지 마세요. 감정 부재를 설명하는 문장도 금지입니다
            - 클리셰·정형 문구 금지

            반드시 JSON만: {"content": "..."}
            """, guide, diariesSummaryJson);

        String resp = callJsonApi(system, user, "generatePlaceReportContent", 0.55);
        try {
            JsonNode node = objectMapper.readTree(resp);
            return node.path("content").asText("이번 달의 장소 기록을 분석했습니다.");
        } catch (Exception e) {
            log.error("generatePlaceReportContent 파싱 실패: {}", e.getMessage());
            return "이번 달의 장소 기록을 분석했습니다.";
        }
    }

    @Override
    public String refineReportGuide(String existingMd, String reportContent, String feedback, int rating, int maxLines) {
        String system = """
            당신은 사용자의 리포트 취향을 파악하고 기록하는 전문가입니다.
            피드백을 통해 사용자가 어떤 스타일의 리포트를 좋아하는지 학습하여 가이드를 정리합니다.""";
        String reportSection = (reportContent != null && !reportContent.isBlank())
            ? "\n[피드백 대상 리포트 본문]\n" + reportContent + "\n" : "";
        String user = String.format("""
            아래 기존 리포트 가이드와 새 피드백을 종합해 %d줄 이내 마크다운 가이드로 재정리해주세요.
            %s
            규칙:
            - 첫 줄: "# 리포트 작성 취향" 헤더
            - 나머지: "- " 불릿 항목 (각 항목 1줄, 핵심만)
            - 기존 항목은 최대한 보존하고, 새 피드백과 명백히 모순되는 항목만 교체
            - 중복 항목은 통합하되 의미 손실 없이
            - 새 피드백이 기존과 모순되면 새 내용 우선
            - 총 %d줄 절대 초과 금지
            - 마크다운 그대로 출력 (코드블록 감싸지 말 것)

            [기존 가이드]
            %s

            [새 피드백] 별점: %d/5, 의견: %s
            """, maxLines, reportSection, maxLines,
            existingMd == null || existingMd.isBlank() ? "(없음)" : existingMd,
            rating, feedback == null ? "" : feedback);

        return callTextApi(system, user, "refineReportGuide");
    }

    private String callJsonApi(String system, String user, String operation, double temperature) {
        Map<String, Object> body = Map.of(
            "model", model,
            "messages", List.of(
                Map.of("role", "system", "content", system),
                Map.of("role", "user", "content", user)
            ),
            "response_format", Map.of("type", "json_object"),
            "temperature", temperature,
            "max_tokens", 1024
        );
        return callApi(body, operation);
    }

    private String callTextApi(String system, String user, String operation) {
        Map<String, Object> body = Map.of(
            "model", model,
            "messages", List.of(
                Map.of("role", "system", "content", system),
                Map.of("role", "user", "content", user)
            ),
            "temperature", 0.3,
            "max_tokens", 1024
        );
        return callApi(body, operation);
    }

    private String callApi(Map<String, Object> body, String operation) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new BadRequestException("OPENAI_API_KEY 미설정");
        }
        long t0 = System.currentTimeMillis();
        try {
            String raw = webClient.post()
                .uri(endpoint + "/chat/completions")
                .header("Authorization", "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(String.class)
                .block();
            log.info("OpenAI {} latency={}ms", operation, System.currentTimeMillis() - t0);
            JsonNode root = objectMapper.readTree(raw);
            return root.path("choices").path(0).path("message").path("content").asText("");
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            log.error("OpenAI {} 호출 실패: {}", operation, e.getMessage());
            throw new BadRequestException("OpenAI 호출 실패: " + e.getMessage());
        }
    }
}
