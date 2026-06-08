package com.plog.api.llm;

import java.util.List;

import com.plog.api.domain.aiguide.Persona;
import com.plog.api.pipeline.dto.AnsweredQa;
import com.plog.api.pipeline.dto.BatchQuestion;
import com.plog.api.pipeline.dto.BatchQuestionsResponse;
import com.plog.api.pipeline.dto.ChatResponse;
import com.plog.api.pipeline.dto.ChatTurn;
import com.plog.api.pipeline.dto.ImagePart;
import com.plog.api.pipeline.dto.VisionResult;

public interface GeminiClient {

    /**
     * 이미지 한 장을 Gemini Vision으로 분석하여 구조화된 결과를 반환한다.
     */
    VisionResult analyzeImage(byte[] imageBytes, String mimeType);

    /**
     * 다중 이미지를 컨텍스트로 멀티턴 대화 진행.
     *
     * @param images           모든 사진 (시간순 정렬 권장) + 각 사진 캡션
     * @param history          현재까지 대화 (USER/MODEL ChatTurn)
     * @param userTurnCount    사용자 발화 누적 횟수
     * @param requiredMinTurns ready_for_draft=true가 허용되는 최소 사용자 답변 수
     */
    ChatResponse continueConversation(List<ImagePart> images, List<ChatTurn> history,
                                      int userTurnCount, int requiredMinTurns);

    /**
     * 페르소나 + 사용자 가이드 MD + 사진 vision + 답변/대화 history → 한국어 일기 초안.
     * @param personaSystemPrompt 페르소나 톤 가이드 (Persona.systemPromptFragment)
     * @param userDiaryGuideMd 사용자 학습 가이드 MD (없으면 null/빈 문자열)
     * @param contextDescription 사진/답변/대화 요약 텍스트
     * @return 한국어 일기 초안 (180~400자 권장)
     */
    String generateDraft(String personaSystemPrompt, String userDiaryGuideMd, String contextDescription);

    /**
     * 기존 가이드 MD + 새 피드백을 종합해 maxLines 이내 한국어 가이드로 재정리.
     */
    String refineDiaryGuide(String existingMd, String newFeedback, int satisfactionScore, int maxLines);

    /**
     * BATCH 모드 — 다중 이미지 + 페르소나 + 질문 수를 받아
     * 한국어 가이드 질문 N개와 각 질문당 답변 후보 3개를 생성.
     */
    BatchQuestionsResponse generateBatchQuestions(
            List<ImagePart> images, Persona persona, int questionCount);

    BatchQuestion generateNextQuestion(List<ImagePart> images, Persona persona,
            List<AnsweredQa> priorAnswers, int nextOrderIdx, int targetCount);
}
