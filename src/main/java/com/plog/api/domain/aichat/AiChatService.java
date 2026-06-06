package com.plog.api.domain.aichat;

import com.plog.api.domain.user.User;
import com.plog.api.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiChatService {

    private final AiChatSessionRepository sessionRepository;
    private final AiChatMessageRepository messageRepository;
    private final AiChatContextRepository contextRepository;
    private final UserRepository userRepository;
    private final AiChatGeminiClient geminiClient;
    private final JdbcTemplate jdbcTemplate;

    @Transactional
    public Map<String, Object> startSession(Long userId, String type, String date) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        AiChatSession session = AiChatSession.of(user, type);
        sessionRepository.save(session);

        String aiOpening = "";

        if ("DIARY_ASSIST".equals(type)) {
            String diaryContext = (date != null)
                    ? buildDiaryContextByDate(userId, date)
                    : buildDiaryContext(userId);
            log.info("diaryContext: {}", diaryContext);
            String systemPrompt = buildDiarySystemPrompt(diaryContext);
            String trigger = "사용자의 일기를 읽고 따뜻하게 대화를 시작해줘. 일기 내용을 언급하며 자연스럽게 안부를 물어봐.";
            log.info("aiOpening 생성 시작");
            aiOpening = geminiClient.chat(systemPrompt, new ArrayList<>(), trigger);
            log.info("aiOpening 결과: {}", aiOpening);
        } else {
            String systemPrompt = buildFreeChatSystemPrompt();
            String trigger = "사용자에게 친근하게 첫 인사를 해줘.";
            log.info("aiOpening 생성 시작 (FREE_CHAT)");
            aiOpening = geminiClient.chat(systemPrompt, new ArrayList<>(), trigger);
            log.info("aiOpening 결과: {}", aiOpening);
        }

        AiChatMessage aiMsg = AiChatMessage.of(session, "AI", aiOpening, "TEXT");
        messageRepository.save(aiMsg);

        return Map.of(
            "sessionId", session.getId(),
            "type", type,
            "aiResponse", aiOpening
        );
    }

    @Transactional
    public Map<String, Object> sendMessage(Long sessionId, Long userId, String userMessage) {
        AiChatSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"));

        // 1. 히스토리 먼저 조회 (현재 메시지 저장 전)
        List<AiChatMessage> history = messageRepository.findBySessionOrderByCreatedAtAsc(session);
        List<Map<String, Object>> geminiHistory = new ArrayList<>();
        for (AiChatMessage msg : history) {
            String role = "USER".equals(msg.getSender()) ? "user" : "model";
            geminiHistory.add(Map.of(
                "role", role,
                "parts", List.of(Map.of("text", msg.getMessage()))
            ));
        }

        // 2. 사용자 메시지 저장
        AiChatMessage userMsg = AiChatMessage.of(session, "USER", userMessage, "TEXT");
        messageRepository.save(userMsg);

        // 3. 세션 타입에 따라 시스템 프롬프트 분기
        String systemPrompt;
        if ("DIARY_ASSIST".equals(session.getType())) {
            String diaryContext = buildDiaryContext(userId);
            systemPrompt = buildDiarySystemPrompt(diaryContext);
        } else {
            systemPrompt = buildFreeChatSystemPrompt();
        }

        // 4. Gemini 호출
        String aiResponse = geminiClient.chat(systemPrompt, geminiHistory, userMessage);

        // 5. AI 응답 저장
        AiChatMessage aiMsg = AiChatMessage.of(session, "AI", aiResponse, "TEXT");
        messageRepository.save(aiMsg);

        return Map.of(
            "userMessage", userMessage,
            "aiResponse", aiResponse
        );
    }

    // 일기 기반 시스템 프롬프트
    private String buildDiarySystemPrompt(String diaryContext) {
        return """
                당신은 사용자의 일기를 바탕으로 대화하는 AI 친구입니다.
                사용자가 털어놓기 어려운 이야기를 편하게 나눌 수 있도록 도와주세요.
                따뜻하고 공감적인 태도로 대화하되, 위험 신호(자해, 극단적 선택 등)가 감지되면
                전문 기관(정신건강 위기상담 전화 1577-0199)을 안내해주세요.
                
                [사용자의 최근 일기 및 감정 데이터]
                """ + diaryContext;
    }

    // 자유 대화 시스템 프롬프트
    private String buildFreeChatSystemPrompt() {
        return """
                당신은 사용자와 자유롭게 대화하는 AI 친구입니다.
                친근하고 유쾌한 태도로 대화하되, 위험 신호(자해, 극단적 선택 등)가 감지되면
                전문 기관(정신건강 위기상담 전화 1577-0199)을 안내해주세요.
                """;
    }

    // 최근 7일 일기 + 감정 데이터 조회
    private String buildDiaryContext(Long userId) {
        try {
            List<Map<String, Object>> diaries = jdbcTemplate.queryForList(
                """
                SELECT d.content, d.created_at,
                       e.sentiment, e.primary_emotion, e.intensity
                FROM diary d
                LEFT JOIN emotion_analysis e ON d.id = e.diary_id
                WHERE d.user_id = ?
                  AND d.created_at >= NOW() - INTERVAL 7 DAY
                ORDER BY d.created_at DESC
                """, userId
            );

            if (diaries.isEmpty()) return "최근 7일간 작성된 일기가 없습니다.";

            StringBuilder sb = new StringBuilder();
            for (Map<String, Object> diary : diaries) {
                sb.append("날짜: ").append(diary.get("created_at")).append("\n");
                sb.append("내용: ").append(diary.get("content")).append("\n");
                if (diary.get("primary_emotion") != null) {
                    sb.append("감정: ").append(diary.get("primary_emotion"))
                      .append(" (강도: ").append(diary.get("intensity")).append(")\n");
                }
                sb.append("---\n");
            }
            return sb.toString();
        } catch (Exception e) {
            log.warn("일기 데이터 조회 실패: {}", e.getMessage());
            return "일기 데이터를 불러올 수 없습니다.";
        }
    }

    private String buildDiaryContextByDate(Long userId, String date) {
        try {
            List<Map<String, Object>> diaries = jdbcTemplate.queryForList(
                """
                SELECT d.content, d.created_at,
                    e.sentiment, e.primary_emotion, e.intensity
                FROM diary d
                LEFT JOIN emotion_analysis e ON d.id = e.diary_id
                WHERE d.user_id = ?
                AND DATE(d.created_at) = ?
                ORDER BY d.created_at DESC
                """, userId, date
            );

            if (diaries.isEmpty()) return date + "에 작성된 일기가 없습니다.";

            StringBuilder sb = new StringBuilder();
            for (Map<String, Object> diary : diaries) {
                sb.append("날짜: ").append(diary.get("created_at")).append("\n");
                sb.append("내용: ").append(diary.get("content")).append("\n");
                if (diary.get("primary_emotion") != null) {
                    sb.append("감정: ").append(diary.get("primary_emotion"))
                    .append(" (강도: ").append(diary.get("intensity")).append(")\n");
                }
                sb.append("---\n");
            }
            return sb.toString();
        } catch (Exception e) {
            log.warn("일기 데이터 조회 실패: {}", e.getMessage());
            return "일기 데이터를 불러올 수 없습니다.";
        }
    }

    @Transactional(readOnly = true)
    public List<AiChatMessage> getMessages(Long sessionId) {
        AiChatSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"));
        return messageRepository.findBySessionOrderByCreatedAtAsc(session);
    }

    @Transactional
    public void endSession(Long sessionId) {
        sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"));
    }
}

