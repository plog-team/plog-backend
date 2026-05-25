package com.plog.api.domain.aiguide;

import org.springframework.stereotype.Component;

import com.plog.api.domain.aiguide.dto.CreateSessionRequest;
import com.plog.api.domain.aiguide.dto.CreateSessionResponse;
import com.plog.api.domain.aiguide.dto.DraftResponse;
import com.plog.api.domain.aiguide.dto.SendChatRequest;
import com.plog.api.domain.aiguide.dto.SendChatResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * LangGraph 개념을 Java state machine으로 구현한 AI 가이드 오케스트레이터.
 * 실제 노드 호출·트랜잭션은 AiSessionService가 담당하며, 본 클래스는 API 진입점 역할.
 *
 * 7-노드 흐름:
 *   1) ImageImport       — PhotoService.upload (멀티파트 + SHA-256 + 리사이즈)
 *   2) ExifExtract       — ExifExtractNode.extract
 *   3) ContextEnrich     — ContextEnrichNode.enrich (촬영 시간 → 계절/요일 + GPS → 지명)
 *   4) VisionAnalysis    — VisionAnalysisNode.analyze (캐시 hit/miss + Gemini)
 *   5) GuideQuestion     — GuideQuestionNode.generate (BATCH 모드)
 *      OR continueConv   — GeminiClient.continueConversation (CONVERSATION 모드)
 *   6) DraftGenerate     — GeminiDraftNode (페르소나·학습 가이드 반영)
 *   7) Feedback          — submitFeedback + DiaryGuideService.appendFromFeedback (학습 누적)
 *
 * State 전이: AiSession.status (ACTIVE → COMPLETED / ABANDONED)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiGuideOrchestrator {

    private final AiSessionService sessionService;

    /** 사진 분석 + 모드별 질문 생성 또는 대화 시작. */
    public CreateSessionResponse startSession(long userId, CreateSessionRequest req) {
        log.info("Orchestrator.start userId={} mode={} persona={} photos={}",
                userId, req.modeOrDefault(), req.personaOrDefault(),
                req.photoIds() == null ? 0 : req.photoIds().size());
        return sessionService.createSession(userId, req);
    }

    /** CONVERSATION 모드 대화 한 턴 처리. */
    public SendChatResponse continueConversation(long userId, long sessionId, SendChatRequest req) {
        return sessionService.sendChat(userId, sessionId, req);
    }

    /** 페르소나·학습 가이드를 반영해 일기 초안 생성. */
    public DraftResponse generateDraft(long userId, long sessionId) {
        return sessionService.generateDraft(userId, sessionId);
    }
}
