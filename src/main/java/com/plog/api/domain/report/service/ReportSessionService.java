package com.plog.api.domain.report.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.plog.api.common.exception.BadRequestException;
import com.plog.api.common.exception.NotFoundException;
import com.plog.api.domain.report.dto.request.ClarifyRequest;
import com.plog.api.domain.report.dto.request.ReportFeedbackRequest;
import com.plog.api.domain.report.dto.response.*;
import com.plog.api.domain.report.entity.*;
import com.plog.api.domain.report.repository.*;
import com.plog.api.domain.user.User;
import com.plog.api.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportSessionService {

    private final ReportSessionRepository sessionRepository;
    private final ReportInterruptRepository interruptRepository;
    private final ReportFeedbackRepository feedbackRepository;
    private final UserRepository userRepository;
    private final ReportOrchestrator orchestrator;
    private final ReportGuideService guideService;
    private final ObjectMapper objectMapper;

    @Transactional
    public GenerateReportResponse generate(Long userId, ReportType type) {
        LocalDate today = LocalDate.now();
        LocalDate periodStart, periodEnd;
        if (type == ReportType.EMOTION) {
            periodStart = today.minusDays(6);
            periodEnd = today;
        } else {
            LocalDate lastMonth = today.minusMonths(1);
            periodStart = lastMonth.withDayOfMonth(1);
            periodEnd = lastMonth.withDayOfMonth(lastMonth.lengthOfMonth());
        }

        ReportSession session = new ReportSession();
        session.setUserId(userId);
        session.setThreadId(UUID.randomUUID().toString());
        session.setReportType(type);
        session.setStatus(ReportStatus.RUNNING);
        session.setPeriodStart(periodStart);
        session.setPeriodEnd(periodEnd);
        session = sessionRepository.save(session);

        final Long sessionId = session.getId();
        final String threadId = session.getThreadId();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                orchestrator.analyzeAsync(sessionId);
            }
        });
        log.info("generate 완료 threadId={} userId={} type={}", threadId, userId, type);
        return new GenerateReportResponse(threadId);
    }

    @Transactional(readOnly = true)
    public ReportStatusResponse getStatus(String threadId, Long userId) {
        ReportSession session = findSession(threadId);
        String userName = userRepository.findById(userId).map(User::getName).orElse("사용자");

        return switch (session.getStatus()) {
            case INTERRUPTED -> buildInterruptedResponse(session, threadId, userName);
            case DONE -> buildDoneResponse(session, threadId, userName);
            case ERROR -> new ReportStatusResponse("error", threadId, userName, null, null, null,
                session.getErrorMessage() != null ? session.getErrorMessage() : "알 수 없는 오류");
            default -> new ReportStatusResponse("running", threadId, userName, null, null, null, null);
        };
    }

    @Transactional
    public void clarify(String threadId, Long userId, ClarifyRequest req) {
        ReportSession session = findSession(threadId);
        if (session.getStatus() != ReportStatus.INTERRUPTED) {
            throw new BadRequestException("현재 인터럽트 상태가 아닙니다.");
        }
        ReportInterrupt interrupt = interruptRepository
            .findFirstBySessionIdAndAnswerIsNullOrderByOrderIdxAsc(session.getId())
            .orElseThrow(() -> new BadRequestException("처리할 인터럽트가 없습니다."));

        interrupt.setAnswer(req.answer());
        interruptRepository.save(interrupt);

        if (!interruptRepository.existsBySessionIdAndAnswerIsNull(session.getId())) {
            session.setStatus(ReportStatus.RUNNING);
            sessionRepository.save(session);
            final Long sessionId = session.getId();
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    orchestrator.generateAsync(sessionId);
                }
            });
            log.info("clarify 완료 → 모든 인터럽트 해소, Phase 3 시작 sessionId={}", session.getId());
        }
    }

    @Transactional
    public void submitFeedback(String threadId, Long userId, ReportFeedbackRequest req) {
        ReportSession session = findSession(threadId);
        ReportFeedback feedback = new ReportFeedback();
        feedback.setSessionId(session.getId());
        feedback.setUserId(userId);
        feedback.setRating(req.rating());
        feedback.setComment(req.comment());
        feedbackRepository.save(feedback);
        String reportContent = extractReportContent(session);
        guideService.appendFromFeedback(userId, req.rating(), req.comment(), reportContent, session.getReportType());
    }

    private String extractReportContent(ReportSession session) {
        if (session.getReportJson() == null) return null;
        try {
            return objectMapper.readTree(session.getReportJson()).path("content").asText(null);
        } catch (Exception e) {
            return null;
        }
    }

    private ReportStatusResponse buildInterruptedResponse(ReportSession session, String threadId, String userName) {
        ReportInterrupt interrupt = interruptRepository
            .findFirstBySessionIdAndAnswerIsNullOrderByOrderIdxAsc(session.getId())
            .orElse(null);
        if (interrupt == null) {
            return new ReportStatusResponse("running", threadId, userName, null, null, null, null);
        }
        List<String> options;
        try {
            options = objectMapper.readValue(interrupt.getOptionsJson(), new TypeReference<>() {});
        } catch (Exception e) {
            options = List.of();
        }
        return new ReportStatusResponse("interrupted", threadId, userName, null, null,
            new InterruptPayload(interrupt.getDiaryId(), interrupt.getDiaryDate().toString(),
                interrupt.getQuestion(), options), null);
    }

    private ReportStatusResponse buildDoneResponse(ReportSession session, String threadId, String userName) {
        try {
            if (session.getReportType() == ReportType.EMOTION) {
                EmotionReportData report = objectMapper.readValue(session.getReportJson(), EmotionReportData.class);
                return new ReportStatusResponse("done", threadId, userName, null, report, null, null);
            } else {
                PlaceReportData report = objectMapper.readValue(session.getReportJson(), PlaceReportData.class);
                return new ReportStatusResponse("done", threadId, userName, report, null, null, null);
            }
        } catch (Exception e) {
            log.error("리포트 JSON 파싱 실패 threadId={}: {}", threadId, e.getMessage());
            return new ReportStatusResponse("error", threadId, userName, null, null, null, "리포트 데이터 오류");
        }
    }

    private ReportSession findSession(String threadId) {
        return sessionRepository.findByThreadId(threadId)
            .orElseThrow(() -> new NotFoundException("리포트 세션을 찾을 수 없습니다: " + threadId));
    }
}
