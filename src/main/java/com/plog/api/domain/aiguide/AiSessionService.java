package com.plog.api.domain.aiguide;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.plog.api.common.exception.BadRequestException;
import com.plog.api.common.exception.NotFoundException;
import com.plog.api.domain.aiguide.dto.AnswerRequest;
import com.plog.api.domain.aiguide.dto.AnswerResponse;
import com.plog.api.domain.aiguide.dto.ChatMessageDto;
import com.plog.api.domain.aiguide.dto.CreateSessionRequest;
import com.plog.api.domain.aiguide.dto.CreateSessionResponse;
import com.plog.api.domain.aiguide.dto.DraftResponse;
import com.plog.api.domain.aiguide.dto.FeedbackRequest;
import com.plog.api.domain.aiguide.dto.GuideQuestionDto;
import com.plog.api.domain.aiguide.dto.PhotoAnalysisDto;
import com.plog.api.domain.aiguide.dto.SendChatRequest;
import com.plog.api.domain.aiguide.dto.SendChatResponse;
import com.plog.api.domain.aiguide.dto.SessionDetailResponse;
import com.plog.api.domain.aiguide.dto.SessionSummaryDto;
import com.plog.api.domain.cache.ImageAnalysisCache;
import com.plog.api.domain.cache.ImageAnalysisCacheRepository;
import com.plog.api.domain.photo.Photo;
import com.plog.api.domain.photo.PhotoRepository;
import com.plog.api.llm.GeminiClient;
import com.plog.api.pipeline.ContextEnrichNode;
import com.plog.api.pipeline.DraftGenerateNode;
import com.plog.api.pipeline.ExifExtractNode;
import com.plog.api.pipeline.GeminiDraftNode;
import com.plog.api.pipeline.GuideQuestionNode;
import com.plog.api.pipeline.VisionAnalysisNode;
import com.plog.api.pipeline.dto.AnsweredQa;
import com.plog.api.pipeline.dto.BatchQuestion;
import com.plog.api.pipeline.dto.BatchQuestionsResponse;
import com.plog.api.pipeline.dto.ChatResponse;
import com.plog.api.pipeline.dto.ChatTurn;
import com.plog.api.pipeline.dto.ContextResult;
import com.plog.api.pipeline.dto.ExifResult;
import com.plog.api.pipeline.dto.ImagePart;
import com.plog.api.pipeline.dto.VisionResult;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiSessionService {

    private static final String STATE_LAST_SESSION = "last_completed_session_id";
    private static final String STATE_CONFIRMED_COUNT = "confirmed_diary_count";

    private final AiSessionRepository sessionRepository;
    private final GuideQuestionRepository questionRepository;
    private final UserFeedbackRepository feedbackRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final PhotoRepository photoRepository;
    private final ExifExtractNode exifExtractNode;
    private final ContextEnrichNode contextEnrichNode;
    private final VisionAnalysisNode visionAnalysisNode;
    private final GuideQuestionNode guideQuestionNode;
    private final DraftGenerateNode draftGenerateNode;
    private final GeminiDraftNode geminiDraftNode;
    private final StateMemoryService stateMemoryService;
    private final DiaryGuideService diaryGuideService;
    private final ImageAnalysisCacheRepository imageAnalysisCacheRepository;
    private final GeminiClient geminiClient;
    private final ObjectMapper objectMapper;

    @Transactional
    public CreateSessionResponse createSession(long userId, CreateSessionRequest req) {
        if (req.photoIds() == null || req.photoIds().isEmpty()) {
            throw new BadRequestException("photoIds는 1개 이상 필요합니다");
        }
        List<Photo> photos = photoRepository.findAllById(req.photoIds());
        if (photos.size() != req.photoIds().size()) {
            throw new NotFoundException("일부 photoId를 찾을 수 없습니다 (요청 " + req.photoIds().size() + "개 중 " + photos.size() + "개만 존재)");
        }
        for (Photo p : photos) {
            if (!p.getUserId().equals(userId)) {
                throw new BadRequestException("photoId=" + p.getId() + "은 본인 소유가 아닙니다");
            }
        }

        // vision은 리사이즈본, EXIF는 원본 bytes에서 추출
        List<PhotoAnalysisDto> analyses = new ArrayList<>();
        List<ImagePart> imageParts = new ArrayList<>();
        List<VisionResult> allVisions = new ArrayList<>();
        List<ExifResult> allExifs = new ArrayList<>();
        VisionResult firstVision = null;
        for (Photo p : photos) {
            byte[] bytes;
            try {
                bytes = Files.readAllBytes(Paths.get(p.getStoredPath()));
            } catch (IOException e) {
                throw new BadRequestException("저장된 이미지 읽기 실패 photoId=" + p.getId() + ": " + e.getMessage());
            }
            // Thumbnailator 리사이즈가 EXIF를 제거하므로 원본 bytes에서 추출
            ExifResult exif = exifExtractNode.extract(readOriginalOrFallback(p, bytes));
            ContextResult context = contextEnrichNode.enrich(exif);
            log.info("Context photoId={} season={} dayOfWeek={} weather={}",
                    p.getId(), context.season(), context.dayOfWeek(), context.weather());
            VisionAnalysisNode.Output out = visionAnalysisNode.analyze(bytes, p.getSha256(), p.getMimeType());
            if (firstVision == null) firstVision = out.vision();  // BATCH 모드 질문 생성용
            String caption = buildImageCaption(imageParts.size() + 1, out.vision(), exif);
            imageParts.add(new ImagePart(bytes, p.getMimeType(), caption));
            allVisions.add(out.vision());
            allExifs.add(exif);
            analyses.add(PhotoAnalysisDto.builder()
                    .photoId(p.getId())
                    .sha256(p.getSha256())
                    .exif(exif)
                    .vision(out.vision())
                    .cacheHit(out.cacheHit())
                    .latencyMs(out.latencyMs())
                    .build());
        }

        String csv = photos.stream().map(p -> String.valueOf(p.getId())).collect(Collectors.joining(","));
        AiSession.Mode mode = req.modeOrDefault();
        Persona persona = req.personaOrDefault();
        AiSession session = sessionRepository.save(AiSession.builder()
                .userId(userId)
                .status(AiSession.Status.ACTIVE)
                .mode(mode)
                .persona(persona)
                .photoIdsCsv(csv)
                .build());

        if (mode == AiSession.Mode.CONVERSATION) {
            // CONVERSATION: 첫 AI 인삿말 생성 + ChatMessage 저장 — 모든 사진 + 캡션 전달
            int requiredMinTurns = Math.max(4, Math.min(6, imageParts.size() * 2 + 1));
            ChatResponse first = geminiClient.continueConversation(imageParts, List.of(), 0, requiredMinTurns);
            ChatMessage assistant = chatMessageRepository.save(ChatMessage.builder()
                    .sessionId(session.getId())
                    .role(ChatMessage.Role.ASSISTANT)
                    .content(first.text())
                    .orderIdx(1)
                    .build());
            log.info("Created CONVERSATION session id={} firstAssistantMsg_len={}",
                    session.getId(), first.text().length());
            return CreateSessionResponse.builder()
                    .sessionId(session.getId())
                    .status(session.getStatus().name())
                    .mode(mode.name())
                    .firstAssistantMessage(assistant.getContent())
                    .photos(analyses)
                    .build();
        }

        int targetCount = computeTargetCount(imageParts.size());
        BatchQuestion firstQ = generateNextQuestionSafe(imageParts, persona, List.of(), 1, targetCount, firstVision);
        GuideQuestion q1 = persistQuestion(session.getId(), 1, firstQ);
        log.info("Created BATCH session id={} userId={} photos={} targetCount={}",
                session.getId(), userId, csv, targetCount);
        return CreateSessionResponse.builder()
                .sessionId(session.getId())
                .status(session.getStatus().name())
                .mode(mode.name())
                .questions(List.of(GuideQuestionDto.from(q1)))
                .photos(analyses)
                .build();
    }

    private int computeTargetCount(int photoCount) {
        return Math.max(5, Math.min(10, (int) Math.ceil(photoCount * 2.5)));
    }

    private int countPhotos(AiSession s) {
        if (s.getPhotoIdsCsv() == null || s.getPhotoIdsCsv().isBlank()) return 0;
        int n = 0;
        for (String part : s.getPhotoIdsCsv().split(",")) {
            if (!part.trim().isEmpty()) n++;
        }
        return n;
    }

    private boolean isFinishIntent(String msg) {
        if (msg == null) return false;
        String m = msg.replaceAll("\\s", "");
        String[] kw = {"초안", "그만", "충분", "정리해", "이제만들", "마무리", "끝낼", "끝내", "다썼"};
        for (String k : kw) {
            if (m.contains(k)) return true;
        }
        return false;
    }

    private GuideQuestion persistQuestion(long sessionId, int orderIdx, BatchQuestion bq) {
        String suggestedJson = null;
        try {
            suggestedJson = objectMapper.writeValueAsString(
                    bq.suggestedAnswers() == null ? List.of() : bq.suggestedAnswers());
        } catch (Exception je) {
            log.warn("suggestedAnswers JSON 직렬화 실패: {}", je.getMessage());
        }
        return questionRepository.save(GuideQuestion.builder()
                .sessionId(sessionId)
                .orderIdx(orderIdx)
                .question(bq.text())
                .questionType(bq.type() == null ? com.plog.api.domain.aiguide.QuestionType.SITUATION : bq.type())
                .suggestedAnswersJson(suggestedJson)
                .build());
    }

    private BatchQuestion generateNextQuestionSafe(List<ImagePart> imageParts, Persona persona,
            List<AnsweredQa> prior, int orderIdx, int targetCount, VisionResult firstVisionOrNull) {
        try {
            return geminiClient.generateNextQuestion(imageParts, persona, prior, orderIdx, targetCount);
        } catch (Exception e) {
            log.warn("Gemini 다음 질문 생성 실패 → 결정론 fallback. orderIdx={} cause={}", orderIdx, e.getMessage());
            List<BatchQuestion> pool = guideQuestionNode.generateFallback(firstVisionOrNull, targetCount).questions();
            int idx = Math.min(Math.max(orderIdx - 1, 0), pool.size() - 1);
            if (!pool.isEmpty()) return pool.get(idx);
            return new BatchQuestion(
                    "사진 속 한 장면에서 가장 인상 깊었던 한 가지는 무엇인가요?",
                    com.plog.api.domain.aiguide.QuestionType.EMOTION,
                    List.of("주변 분위기가 좋았다.", "함께 있던 사람들이 좋았다.", "공간 자체가 인상 깊었다."));
        }
    }

    @Transactional
    public SendChatResponse sendChat(long userId, long sessionId, SendChatRequest req) {
        AiSession s = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new NotFoundException("세션을 찾을 수 없습니다 id=" + sessionId));
        if (!s.getUserId().equals(userId)) throw new BadRequestException("본인 세션이 아닙니다");
        if (s.getMode() != AiSession.Mode.CONVERSATION) {
            throw new BadRequestException("CONVERSATION 모드 세션이 아닙니다 (mode=" + s.getMode() + ")");
        }

        long t0 = System.currentTimeMillis();
        List<ChatMessage> history = chatMessageRepository.findAllBySessionIdOrderByOrderIdxAsc(sessionId);
        int nextIdx = history.size() + 1;

        // user 발화 저장
        ChatMessage userMsg = chatMessageRepository.save(ChatMessage.builder()
                .sessionId(sessionId)
                .role(ChatMessage.Role.USER)
                .content(req.message().trim())
                .orderIdx(nextIdx)
                .build());

        // 전체 사진 + 대화 이력을 Gemini에 전달
        List<ImagePart> imageParts = loadAllImageParts(s);

        List<ChatTurn> turns = new ArrayList<>();
        for (ChatMessage m : history) {
            turns.add(m.getRole() == ChatMessage.Role.USER ? ChatTurn.user(m.getContent()) : ChatTurn.model(m.getContent()));
        }
        turns.add(ChatTurn.user(req.message().trim()));

        long userTurnCount = turns.stream().filter(t -> "user".equals(t.role())).count();
        int requiredMinTurns = Math.max(4, Math.min(6, imageParts.size() * 2 + 1));
        ChatResponse aiResp = geminiClient.continueConversation(imageParts, turns, (int) userTurnCount, requiredMinTurns);

        ChatMessage aMsg = chatMessageRepository.save(ChatMessage.builder()
                .sessionId(sessionId)
                .role(ChatMessage.Role.ASSISTANT)
                .content(aiResp.text())
                .orderIdx(nextIdx + 1)
                .build());

        long elapsed = System.currentTimeMillis() - t0;
        log.info("Chat sessionId={} userTurn={} readyForDraft={} latency={}ms",
                sessionId, userTurnCount, aiResp.readyForDraft(), elapsed);

        boolean ready = aiResp.readyForDraft()
                || (isFinishIntent(req.message()) && userTurnCount >= 3);

        return SendChatResponse.builder()
                .assistantMessage(aMsg.getContent())
                .readyForDraft(ready)
                .userMessageId(userMsg.getId())
                .assistantMessageId(aMsg.getId())
                .latencyMs(elapsed)
                .build();
    }

    /** 세션의 BATCH 질문 목록 조회. */
    @Transactional(readOnly = true)
    public List<GuideQuestionDto> listQuestions(long userId, long sessionId) {
        AiSession s = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new NotFoundException("세션을 찾을 수 없습니다 id=" + sessionId));
        if (!s.getUserId().equals(userId)) {
            throw new BadRequestException("본인 세션이 아닙니다");
        }
        return questionRepository.findAllBySessionIdOrderByOrderIdxAsc(sessionId).stream()
                .map(GuideQuestionDto::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public SessionDetailResponse getSession(long userId, long sessionId) {
        AiSession s = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new NotFoundException("세션을 찾을 수 없습니다 id=" + sessionId));
        if (!s.getUserId().equals(userId)) {
            throw new BadRequestException("본인 세션이 아닙니다");
        }
        SessionDetailResponse.SessionDetailResponseBuilder b = SessionDetailResponse.builder()
                .sessionId(s.getId())
                .userId(s.getUserId())
                .status(s.getStatus().name())
                .mode(s.getMode().name())
                .photoIdsCsv(s.getPhotoIdsCsv())
                .draft(s.getDraft())
                .createdAt(s.getCreatedAt())
                .completedAt(s.getCompletedAt());

        if (s.getMode() == AiSession.Mode.BATCH) {
            b.questions(questionRepository.findAllBySessionIdOrderByOrderIdxAsc(sessionId).stream()
                    .map(GuideQuestionDto::from).toList());
        } else {
            b.chatMessages(chatMessageRepository.findAllBySessionIdOrderByOrderIdxAsc(sessionId).stream()
                    .map(ChatMessageDto::from).toList());
        }
        return b.build();
    }

    @Transactional
    public AnswerResponse answerQuestion(long userId, long sessionId, long questionId, AnswerRequest req) {
        AiSession s = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new NotFoundException("세션을 찾을 수 없습니다 id=" + sessionId));
        if (!s.getUserId().equals(userId)) throw new BadRequestException("본인 세션이 아닙니다");
        if (s.getMode() != AiSession.Mode.BATCH) {
            throw new BadRequestException("BATCH 모드 세션이 아닙니다");
        }
        GuideQuestion q = questionRepository.findById(questionId)
                .orElseThrow(() -> new NotFoundException("질문을 찾을 수 없습니다 id=" + questionId));
        if (!q.getSessionId().equals(sessionId)) {
            throw new BadRequestException("질문이 해당 세션에 속하지 않습니다");
        }
        q.updateAnswer(req == null ? null : (req.answer() == null ? null : req.answer().trim()));

        List<GuideQuestion> all = questionRepository.findAllBySessionIdOrderByOrderIdxAsc(sessionId);
        List<GuideQuestion> answeredList = all.stream()
                .filter(x -> x.getAnswer() != null && !x.getAnswer().isBlank())
                .toList();
        int answeredCount = answeredList.size();
        int targetCount = computeTargetCount(countPhotos(s));
        boolean done = answeredCount >= targetCount;

        GuideQuestionDto nextDto = null;
        if (!done) {
            GuideQuestion pending = all.stream()
                    .filter(x -> x.getAnswer() == null || x.getAnswer().isBlank())
                    .findFirst().orElse(null);
            if (pending != null) {
                nextDto = GuideQuestionDto.from(pending);
            } else {
                int nextOrderIdx = all.stream().mapToInt(GuideQuestion::getOrderIdx).max().orElse(answeredCount) + 1;
                List<AnsweredQa> prior = answeredList.stream()
                        .map(x -> new AnsweredQa(x.getQuestion(), x.getAnswer()))
                        .toList();
                BatchQuestion bq = generateNextQuestionSafe(
                        loadAllImageParts(s), Persona.orDefault(s.getPersona()), prior, nextOrderIdx, targetCount, null);
                nextDto = GuideQuestionDto.from(persistQuestion(sessionId, nextOrderIdx, bq));
            }
        }
        log.info("Answered sessionId={} qid={} answeredCount={} target={} done={}",
                sessionId, questionId, answeredCount, targetCount, done);
        return AnswerResponse.builder()
                .answered(GuideQuestionDto.from(q))
                .nextQuestion(nextDto)
                .done(done)
                .answeredCount(answeredCount)
                .targetCount(targetCount)
                .build();
    }

    @Transactional
    public DraftResponse generateDraft(long userId, long sessionId) {
        AiSession s = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new NotFoundException("세션을 찾을 수 없습니다 id=" + sessionId));
        if (!s.getUserId().equals(userId)) throw new BadRequestException("본인 세션이 아닙니다");

        if (s.getMode() == AiSession.Mode.CONVERSATION) {
            long userTurns = chatMessageRepository.findAllBySessionIdOrderByOrderIdxAsc(sessionId).stream()
                    .filter(m -> m.getRole() == ChatMessage.Role.USER)
                    .count();
            if (userTurns < 4) {
                throw new BadRequestException("정보가 부족해서 초안을 작성할 수 없습니다.");
            }
        }

        // 모든 사진의 vision + exif 로드
        List<VisionResult> allVisions = new ArrayList<>();
        List<ExifResult> allExifs = new ArrayList<>();
        loadVisionsAndExifs(s, allVisions, allExifs);
        VisionResult firstVision = allVisions.isEmpty() ? null : allVisions.get(0);
        Persona persona = Persona.orDefault(s.getPersona());
        String userGuide = diaryGuideService.getGuide(userId).orElse(null);

        String draft;
        if (s.getMode() == AiSession.Mode.BATCH) {
            List<GuideQuestion> qs = questionRepository.findAllBySessionIdOrderByOrderIdxAsc(sessionId);
            // Gemini 초안 우선, 실패 시 결정론 fallback.
            try {
                draft = geminiDraftNode.generate(persona, userGuide, buildContext(allVisions, allExifs, qs, null));
            } catch (Exception e) {
                log.warn("Gemini draft 실패 → 결정론 fallback 사용. cause={}", e.getMessage());
                draft = draftGenerateNode.generate(firstVision, qs);
            }
        } else {
            List<ChatMessage> msgs = chatMessageRepository.findAllBySessionIdOrderByOrderIdxAsc(sessionId);
            try {
                draft = geminiDraftNode.generate(persona, userGuide, buildContext(allVisions, allExifs, null, msgs));
            } catch (Exception e) {
                log.warn("Gemini draft 실패 → 결정론 fallback 사용 (CONVERSATION). cause={}", e.getMessage());
                List<GuideQuestion> pseudoQs = new ArrayList<>();
                int idx = 0;
                for (ChatMessage m : msgs) {
                    if (m.getRole() == ChatMessage.Role.USER) {
                        idx++;
                        pseudoQs.add(GuideQuestion.builder()
                                .sessionId(sessionId)
                                .orderIdx(idx)
                                .question("(대화)")
                                .answer(m.getContent())
                                .build());
                    }
                }
                draft = draftGenerateNode.generate(firstVision, pseudoQs);
            }
        }
        s.updateDraft(draft);
        return DraftResponse.builder()
                .sessionId(s.getId())
                .draft(draft)
                .charCount(draft.length())
                .build();
    }

    @Transactional
    public SessionDetailResponse confirm(long userId, long sessionId) {
        AiSession s = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new NotFoundException("세션을 찾을 수 없습니다 id=" + sessionId));
        if (!s.getUserId().equals(userId)) throw new BadRequestException("본인 세션이 아닙니다");
        if (s.getDraft() == null || s.getDraft().isBlank()) {
            throw new BadRequestException("draft가 없습니다. 먼저 POST /draft를 호출하세요");
        }
        s.complete();
        stateMemoryService.upsert(userId, STATE_LAST_SESSION, s.getId());
        int prev = stateMemoryService.get(userId, STATE_CONFIRMED_COUNT, Integer.class).orElse(0);
        stateMemoryService.upsert(userId, STATE_CONFIRMED_COUNT, prev + 1);
        log.info("Session confirmed id={} userId={} confirmed_count={}", sessionId, userId, prev + 1);
        return getSession(userId, sessionId);
    }

    @Transactional
    public void submitFeedback(long userId, long sessionId, FeedbackRequest req) {
        AiSession s = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new NotFoundException("세션을 찾을 수 없습니다 id=" + sessionId));
        if (!s.getUserId().equals(userId)) throw new BadRequestException("본인 세션이 아닙니다");
        feedbackRepository.save(UserFeedback.builder()
                .sessionId(sessionId)
                .satisfactionScore(req.satisfactionScore())
                .comment(req.comment())
                .build());
        // 사용자 학습 가이드 MD 자동 누적 (실패해도 피드백 자체는 성공). score null 허용.
        int safeScore = req.satisfactionScore() == null ? 0 : req.satisfactionScore();
        diaryGuideService.appendFromFeedback(userId, safeScore, req.comment());
    }

    /** 사용자별 세션 목록 (본문 미포함 요약). */
    @Transactional(readOnly = true)
    public List<SessionSummaryDto> listUserSessions(long userId, int limit) {
        org.springframework.data.domain.Pageable pageable =
                org.springframework.data.domain.PageRequest.of(0, Math.min(Math.max(limit, 1), 100));
        return sessionRepository.findAllByUserIdOrderByIdDesc(userId, pageable).stream()
                .map(SessionSummaryDto::from)
                .toList();
    }

    /** SHA-256 해시로 Vision 캐시 직접 조회. */
    @Transactional(readOnly = true)
    public ImageAnalysisCache lookupCache(String sha256) {
        return imageAnalysisCacheRepository.findBySha256(sha256).orElse(null);
    }

    /** 세션 사진의 vision + EXIF를 포함한 컨텍스트 빌드. */
    private String buildContext(List<VisionResult> visions, List<ExifResult> exifs,
                                 List<GuideQuestion> questions, List<ChatMessage> chatMessages) {
        StringBuilder sb = new StringBuilder();
        sb.append("날짜: ").append(java.time.LocalDate.now()).append("\n");
        if (visions != null && !visions.isEmpty()) {
            sb.append("[사진 분석 — 총 ").append(visions.size()).append("장]\n");
            for (int i = 0; i < visions.size(); i++) {
                VisionResult v = visions.get(i);
                ExifResult ex = (exifs != null && i < exifs.size()) ? exifs.get(i) : null;
                sb.append("\n사진").append(i + 1).append(":\n");
                if (ex != null && ex.capturedAt() != null) {
                    // 분 단위 정확 시각 대신 친숙한 표현만 노출
                    sb.append("- 촬영 시점: ").append(timePhase(ex.capturedAt())).append('\n');
                }
                if (ex != null && ex.latitude() != null && ex.longitude() != null) {
                    sb.append("- 위치(GPS): 위도 ").append(String.format("%.4f", ex.latitude()))
                      .append(", 경도 ").append(String.format("%.4f", ex.longitude())).append('\n');
                }
                if (v == null) { sb.append("- (vision 분석 결과 없음)\n"); continue; }
                if (v.scene() != null) sb.append("- 장면: ").append(v.scene()).append('\n');
                if (v.mood() != null) sb.append("- 분위기: ").append(v.mood()).append('\n');
                if (v.timeOfDay() != null) sb.append("- 시간대(추정): ").append(v.timeOfDay()).append('\n');
                if (v.weatherHint() != null) sb.append("- 날씨(추정): ").append(v.weatherHint()).append('\n');
                if (v.suggestedEmotion() != null) sb.append("- 추정 감정: ").append(v.suggestedEmotion()).append('\n');
                if (v.objects() != null && !v.objects().isEmpty()) {
                    sb.append("- 사물: ").append(String.join(", ", v.objects())).append('\n');
                }
                if (v.oneLineSummary() != null) sb.append("- 한 줄 요약: ").append(v.oneLineSummary()).append('\n');
            }
            sb.append('\n');
        }
        if (questions != null && !questions.isEmpty()) {
            sb.append("[사용자 답변]\n");
            for (GuideQuestion q : questions) {
                if (q.getAnswer() != null && !q.getAnswer().isBlank()) {
                    sb.append("- Q: ").append(q.getQuestion()).append("\n  A: ").append(q.getAnswer().trim()).append('\n');
                }
            }
        }
        if (chatMessages != null && !chatMessages.isEmpty()) {
            sb.append("[대화 기록]\n");
            for (ChatMessage m : chatMessages) {
                sb.append(m.getRole() == ChatMessage.Role.USER ? "사용자: " : "AI: ")
                  .append(m.getContent()).append('\n');
            }
        }
        return sb.toString();
    }

    /**
     * 세션 사진을 Gemini 멀티 이미지 호출용 ImagePart 리스트로 변환.
     * vision은 리사이즈본, EXIF는 원본 bytes 사용.
     */
    private List<ImagePart> loadAllImageParts(AiSession s) {
        List<ImagePart> result = new ArrayList<>();
        if (s.getPhotoIdsCsv() == null || s.getPhotoIdsCsv().isBlank()) return result;
        String[] ids = s.getPhotoIdsCsv().split(",");
        int order = 0;
        for (String idStr : ids) {
            long pid;
            try { pid = Long.parseLong(idStr.trim()); } catch (Exception e) { continue; }
            Photo p = photoRepository.findById(pid).orElse(null);
            if (p == null) continue;
            try {
                byte[] bytes = Files.readAllBytes(Paths.get(p.getStoredPath()));
                ExifResult exif = exifExtractNode.extract(readOriginalOrFallback(p, bytes));
                VisionAnalysisNode.Output out = visionAnalysisNode.analyze(bytes, p.getSha256(), p.getMimeType());
                String caption = buildImageCaption(++order, out.vision(), exif);
                result.add(new ImagePart(bytes, p.getMimeType(), caption));
            } catch (IOException e) {
                log.warn("이미지 로드 실패 photoId={}: {}", pid, e.getMessage());
            }
        }
        return result;
    }

    /** 세션의 모든 사진 vision + exif 추출 (초안 생성용). EXIF는 원본 bytes에서 읽음. */
    private void loadVisionsAndExifs(AiSession s, List<VisionResult> outVisions, List<ExifResult> outExifs) {
        if (s.getPhotoIdsCsv() == null || s.getPhotoIdsCsv().isBlank()) return;
        for (String idStr : s.getPhotoIdsCsv().split(",")) {
            long pid;
            try { pid = Long.parseLong(idStr.trim()); } catch (Exception e) { continue; }
            Photo p = photoRepository.findById(pid).orElse(null);
            if (p == null) continue;
            try {
                byte[] bytes = Files.readAllBytes(Paths.get(p.getStoredPath()));
                outExifs.add(exifExtractNode.extract(readOriginalOrFallback(p, bytes)));
                outVisions.add(visionAnalysisNode.analyze(bytes, p.getSha256(), p.getMimeType()).vision());
            } catch (IOException e) {
                log.warn("vision/exif 재조회 실패 photoId={}: {}", pid, e.getMessage());
            }
        }
    }

    /**
     * EXIF 보존된 원본 bytes 반환. 없으면 리사이즈본으로 fallback.
     * 원본 경로: {sha256}.original
     */
    private byte[] readOriginalOrFallback(Photo p, byte[] resizedBytes) {
        try {
            Path stored = Paths.get(p.getStoredPath());
            Path originalPath = stored.resolveSibling(p.getSha256() + ".original");
            if (Files.exists(originalPath)) {
                return Files.readAllBytes(originalPath);
            }
        } catch (Exception e) {
            log.warn("원본 bytes 로드 실패 photoId={}: {}", p.getId(), e.getMessage());
        }
        return resizedBytes;
    }

    /**
     * 사진 1장을 "(친숙 시간 표현, 한국 지명) 장면" 형식의 캡션 한 줄로 요약.
     */
    private String buildImageCaption(int order, VisionResult vision, ExifResult exif) {
        StringBuilder sb = new StringBuilder();
        sb.append("(");
        if (exif != null && exif.capturedAt() != null) {
            sb.append(timePhase(exif.capturedAt()));
        } else {
            sb.append("시간 미상");
        }
        sb.append(", ");
        String locationHint = (exif == null) ? null : contextEnrichNode.enrich(exif).locationHint();
        if (locationHint != null && !locationHint.isBlank()) {
            sb.append(locationHint);
        } else {
            sb.append("위치 미상");
        }
        sb.append(") ");
        if (vision != null) {
            if (vision.scene() != null) sb.append(vision.scene());
            if (vision.mood() != null) sb.append(" — 분위기: ").append(vision.mood());
            if (vision.suggestedEmotion() != null) sb.append(" / 추정감정: ").append(vision.suggestedEmotion());
        }
        return sb.toString();
    }

    /**
     * 촬영 시간을 "아침", "점심 무렵" 같은 친숙한 표현으로 변환.
     */
    private static String timePhase(java.time.LocalDateTime t) {
        int h = t.getHour();
        if (h < 6) return "이른 새벽";
        if (h < 8) return "이른 아침";
        if (h < 11) return "아침";
        if (h < 13) return "점심 무렵";
        if (h < 16) return "오후";
        if (h < 18) return "늦은 오후";
        if (h < 20) return "저녁";
        if (h < 23) return "밤";
        return "한밤중";
    }
}
