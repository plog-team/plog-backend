package com.plog.api.domain.aiguide;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.plog.api.common.UserContext;
import java.util.List;

import org.springframework.web.bind.annotation.RequestParam;

import com.plog.api.domain.aiguide.dto.AnswerRequest;
import com.plog.api.domain.aiguide.dto.CreateSessionRequest;
import com.plog.api.domain.aiguide.dto.CreateSessionResponse;
import com.plog.api.domain.aiguide.dto.DraftResponse;
import com.plog.api.domain.aiguide.dto.FeedbackRequest;
import com.plog.api.domain.aiguide.dto.GuideQuestionDto;
import com.plog.api.domain.aiguide.dto.SendChatRequest;
import com.plog.api.domain.aiguide.dto.SendChatResponse;
import com.plog.api.domain.aiguide.dto.SessionDetailResponse;
import com.plog.api.domain.aiguide.dto.SessionSummaryDto;
import com.plog.api.domain.cache.ImageAnalysisCache;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/ai-guide/sessions")
@RequiredArgsConstructor
public class AiSessionController {

    private final AiSessionService aiSessionService;
    private final AiGuideOrchestrator orchestrator;

    @PostMapping
    public CreateSessionResponse create(@Valid @RequestBody CreateSessionRequest req) {
        return orchestrator.startSession(UserContext.get(), req);
    }

    /** 사용자별 세션 목록 반환 (본문 미포함 요약). */
    @GetMapping
    public List<SessionSummaryDto> list(@RequestParam(defaultValue = "20") int limit) {
        return aiSessionService.listUserSessions(UserContext.get(), limit);
    }

    /** SHA-256 해시로 Vision 분석 캐시 직접 조회. */
    @GetMapping("/debug/cache")
    public ImageAnalysisCache debugCache(@RequestParam("hash") String sha256) {
        return aiSessionService.lookupCache(sha256);
    }

    @GetMapping("/{sessionId}")
    public SessionDetailResponse get(@PathVariable long sessionId) {
        return aiSessionService.getSession(UserContext.get(), sessionId);
    }

    /** 세션에 생성된 BATCH 가이드 질문 목록 반환. */
    @GetMapping("/{sessionId}/questions")
    public List<GuideQuestionDto> listQuestions(@PathVariable long sessionId) {
        return aiSessionService.listQuestions(UserContext.get(), sessionId);
    }

    @PostMapping("/{sessionId}/questions/{questionId}/answer")
    public GuideQuestionDto answer(@PathVariable long sessionId,
                                   @PathVariable long questionId,
                                   @Valid @RequestBody(required = false) AnswerRequest req) {
        return aiSessionService.answerQuestion(UserContext.get(), sessionId, questionId, req);
    }

    @PostMapping("/{sessionId}/chat")
    public SendChatResponse chat(@PathVariable long sessionId, @Valid @RequestBody SendChatRequest req) {
        return orchestrator.continueConversation(UserContext.get(), sessionId, req);
    }

    @PostMapping("/{sessionId}/draft")
    public DraftResponse draft(@PathVariable long sessionId) {
        return orchestrator.generateDraft(UserContext.get(), sessionId);
    }

    @PostMapping("/{sessionId}/confirm")
    public SessionDetailResponse confirm(@PathVariable long sessionId) {
        return aiSessionService.confirm(UserContext.get(), sessionId);
    }

    @PostMapping("/{sessionId}/feedback")
    public ResponseEntity<Void> feedback(@PathVariable long sessionId,
                                          @Valid @RequestBody FeedbackRequest req) {
        aiSessionService.submitFeedback(UserContext.get(), sessionId, req);
        return ResponseEntity.noContent().build();
    }
}
