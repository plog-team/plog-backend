package com.plog.api.llm;

import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.plog.api.domain.aiguide.Persona;
import com.plog.api.domain.aiguide.QuestionType;
import com.plog.api.pipeline.dto.BatchQuestion;
import com.plog.api.pipeline.dto.BatchQuestionsResponse;
import com.plog.api.pipeline.dto.ChatResponse;
import com.plog.api.pipeline.dto.ChatTurn;
import com.plog.api.pipeline.dto.ImagePart;
import com.plog.api.pipeline.dto.VisionResult;

import java.util.ArrayList;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "plog.gemini", name = "use-mock", havingValue = "true", matchIfMissing = true)
public class GeminiClientMock implements GeminiClient {

    private static final List<String> MOCK_OBJECTS = List.of("커피잔", "테이블", "노트북", "햇살");
    private static final String MOCK_SCENE = "오후의 카페 창가";
    private static final String MOCK_MOOD = "차분하고 따뜻함";
    private static final String MOCK_TIME = "오후";
    private static final String MOCK_WEATHER = "맑음";
    private static final String MOCK_EMOTION = "여유로움";
    private static final String MOCK_SUMMARY = "따뜻한 햇살이 드는 카페에서 보낸 차분한 오후 한때";

    private static final List<String> MOCK_CHAT_QUESTIONS = List.of(
        "사진 보니까 카페 다녀오셨네요. 누구랑 같이 가셨어요?",
        "오, 좋네요. 거기서 뭐 했어요?",
        "기억에 남는 한 가지가 있다면 뭐예요?",
        "마지막으로, 사진을 찍을 때 기분이 어땠어요?"
    );
    private static final String MOCK_CHAT_FINAL = "잘 들었어요! 이제 일기로 정리해볼게요 ✨";

    @Override
    public VisionResult analyzeImage(byte[] imageBytes, String mimeType) {
        log.info("[GeminiClient Mock] analyzeImage called, bytes={}, mime={}", imageBytes.length, mimeType);
        return VisionResult.builder()
                .objects(MOCK_OBJECTS)
                .scene(MOCK_SCENE)
                .mood(MOCK_MOOD)
                .timeOfDay(MOCK_TIME)
                .weatherHint(MOCK_WEATHER)
                .suggestedEmotion(MOCK_EMOTION)
                .oneLineSummary(MOCK_SUMMARY)
                .build();
    }

    @Override
    public ChatResponse continueConversation(List<ImagePart> images, List<ChatTurn> history,
                                              int userTurnCount, int requiredMinTurns) {
        log.info("[Mock] chat turn={}, history={}, images={}, requiredMin={}",
                userTurnCount, history.size(), images == null ? 0 : images.size(), requiredMinTurns);
        if (userTurnCount >= Math.max(requiredMinTurns, MOCK_CHAT_QUESTIONS.size())) {
            return ChatResponse.builder().text(MOCK_CHAT_FINAL).readyForDraft(true).build();
        }
        int idx = Math.min(userTurnCount, MOCK_CHAT_QUESTIONS.size() - 1);
        return ChatResponse.builder()
                .text(MOCK_CHAT_QUESTIONS.get(idx))
                .readyForDraft(false)
                .build();
    }

    @Override
    public String generateDraft(String personaSystemPrompt, String userDiaryGuideMd, String contextDescription) {
        log.info("[Mock] generateDraft persona_len={} guide_len={} ctx_len={}",
                personaSystemPrompt == null ? 0 : personaSystemPrompt.length(),
                userDiaryGuideMd == null ? 0 : userDiaryGuideMd.length(),
                contextDescription == null ? 0 : contextDescription.length());
        return "2026년 5월 17일 일요일\n\n오늘은 카페에서 친구와 만났다. 오랜만에 본 친구와 수다를 떨었다. " +
               "커피와 디저트도 맛있었고 시간 가는 줄 몰랐다.\n\n오늘의 감정 한 단어는 '편안함'.";
    }

    @Override
    public BatchQuestionsResponse generateBatchQuestions(List<ImagePart> images, Persona persona, int questionCount) {
        log.info("[Mock] generateBatchQuestions images={}, persona={}, count={}",
                images == null ? 0 : images.size(), persona, questionCount);
        List<BatchQuestion> qs = new ArrayList<>(questionCount);
        QuestionType[] types = {QuestionType.SITUATION, QuestionType.MEANING, QuestionType.EMOTION};
        for (int i = 0; i < questionCount; i++) {
            qs.add(new BatchQuestion(
                    "Mock 질문 " + (i + 1) + ": 사진 속 한 장면에서 가장 인상 깊었던 한 가지는 무엇인가요?",
                    types[i % types.length],
                    List.of(
                            "답변 후보 A (담백한 사실)",
                            "답변 후보 B (감정 한 줄)",
                            "답변 후보 C (특별한 디테일)"
                    )
            ));
        }
        return new BatchQuestionsResponse(qs);
    }

    @Override
    public String refineDiaryGuide(String existingMd, String newFeedback, int satisfactionScore, int maxLines) {
        log.info("[Mock] refineDiaryGuide existing_lines={} feedback_len={}",
                existingMd == null ? 0 : existingMd.split("\\r?\\n").length,
                newFeedback == null ? 0 : newFeedback.length());
        StringBuilder sb = new StringBuilder();
        sb.append("# 일기 작성 취향\n");
        if (existingMd != null && !existingMd.isBlank()) {
            for (String line : existingMd.split("\\r?\\n")) {
                if (!line.isBlank() && !line.startsWith("# ")) sb.append(line).append('\n');
            }
        }
        sb.append("- (피드백 ").append(satisfactionScore).append("점) ").append(newFeedback.trim()).append('\n');
        return sb.toString();
    }
}
