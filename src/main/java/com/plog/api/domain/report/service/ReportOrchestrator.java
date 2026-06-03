package com.plog.api.domain.report.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.plog.api.domain.diary.Diary;
import com.plog.api.domain.diary.DiaryRepository;
import com.plog.api.domain.report.entity.*;
import com.plog.api.domain.report.repository.*;
import com.plog.api.llm.OpenAiReportClient;
import com.plog.api.pipeline.dto.DiaryAnalysisItem;
import com.plog.api.pipeline.dto.DiaryAnalysisResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportOrchestrator {

    private final ReportSessionRepository sessionRepository;
    private final ReportInterruptRepository interruptRepository;
    private final DiaryRepository diaryRepository;
    private final EmotionReportProcessor emotionProcessor;
    private final PlaceReportProcessor placeProcessor;
    private final ReportGuideService guideService;
    private final OpenAiReportClient openAiReportClient;
    private final ObjectMapper objectMapper;

    @Async("reportTaskExecutor")
    @Transactional
    public void analyzeAsync(Long sessionId) {
        ReportSession session = sessionRepository.findById(sessionId).orElse(null);
        if (session == null) return;
        try {
            List<Diary> diaries = diaryRepository.findByUserIdAndDiaryDateBetweenOrderByDiaryDateAsc(
                session.getUserId(), session.getPeriodStart(), session.getPeriodEnd());

            List<DiaryAnalysisResult> analysisResults;
            List<DiaryAnalysisItem> items = new ArrayList<>();

            if (session.getReportType() == ReportType.EMOTION) {
                analysisResults = emotionProcessor.analyzeForInterrupts(diaries);
                for (int i = 0; i < diaries.size(); i++) {
                    Diary d = diaries.get(i);
                    DiaryAnalysisResult r = analysisResults.get(i);
                    items.add(new DiaryAnalysisItem(d.getId(), d.getDiaryDate().toString(),
                        r.needsClarification() ? null : r.emotions(), null, excerpt(d.getBody())));
                }
            } else {
                analysisResults = placeProcessor.analyzeForInterrupts(diaries);
                for (int i = 0; i < diaries.size(); i++) {
                    Diary d = diaries.get(i);
                    DiaryAnalysisResult r = analysisResults.get(i);
                    List<String> placeEmotions = (r.places() != null && !r.places().isEmpty()
                        && r.emotions() != null && !r.emotions().isEmpty()) ? r.emotions() : null;
                    items.add(new DiaryAnalysisItem(d.getId(), d.getDiaryDate().toString(),
                        placeEmotions, r.needsClarification() ? null : r.places(), excerpt(d.getBody())));
                }
            }

            session.setWorkingDataJson(objectMapper.writeValueAsString(items));

            int orderIdx = 0;
            for (int i = 0; i < diaries.size(); i++) {
                DiaryAnalysisResult r = analysisResults.get(i);
                if (r.needsClarification()) {
                    Diary d = diaries.get(i);
                    ReportInterrupt interrupt = new ReportInterrupt();
                    interrupt.setSessionId(sessionId);
                    interrupt.setDiaryId(d.getId());
                    interrupt.setDiaryDate(d.getDiaryDate());
                    interrupt.setQuestion(r.question());
                    interrupt.setOptionsJson(objectMapper.writeValueAsString(r.options()));
                    interrupt.setOrderIdx(orderIdx++);
                    interruptRepository.save(interrupt);
                }
            }

            if (interruptRepository.existsBySessionIdAndAnswerIsNull(sessionId)) {
                session.setStatus(ReportStatus.INTERRUPTED);
                sessionRepository.save(session);
                log.info("analyzeAsync 완료 → INTERRUPTED sessionId={}", sessionId);
            } else {
                sessionRepository.save(session);
                log.info("analyzeAsync 완료 → 인터럽트 없음, Phase 3 진행 sessionId={}", sessionId);
                performGeneration(session);
            }
        } catch (Exception e) {
            log.error("analyzeAsync 실패 sessionId={}: {}", sessionId, e.getMessage(), e);
            session.setStatus(ReportStatus.ERROR);
            session.setErrorMessage("일기 분석 중 오류가 발생했습니다.");
            sessionRepository.save(session);
        }
    }

    @Async("reportTaskExecutor")
    @Transactional
    public void generateAsync(Long sessionId) {
        ReportSession session = sessionRepository.findById(sessionId).orElse(null);
        if (session == null) return;
        if (session.getStatus() != ReportStatus.RUNNING) {
            log.warn("generateAsync skipped: session {} is in status {}", sessionId, session.getStatus());
            return;
        }
        performGeneration(session);
    }

    private static String excerpt(String body) {
        if (body == null) return null;
        return body.length() <= 150 ? body : body.substring(0, 150) + "…";
    }

    private void performGeneration(ReportSession session) {
        if (session.getWorkingDataJson() == null) {
            session.setStatus(ReportStatus.ERROR);
            session.setErrorMessage("분석 데이터가 없습니다.");
            sessionRepository.save(session);
            return;
        }
        try {
            List<DiaryAnalysisItem> items = objectMapper.readValue(
                session.getWorkingDataJson(), new TypeReference<>() {});

            List<ReportInterrupt> interrupts = interruptRepository
                .findBySessionIdOrderByOrderIdxAsc(session.getId());
            for (ReportInterrupt interrupt : interrupts) {
                if (interrupt.getAnswer() == null) continue;
                items = items.stream().map(item -> {
                    if (!item.diaryId().equals(interrupt.getDiaryId())) return item;
                    if (session.getReportType() == ReportType.EMOTION) {
                        boolean isSkip = OpenAiReportClient.EMOTION_SKIP_ANSWER.equals(interrupt.getAnswer());
                        return item.withEmotions(isSkip ? null : List.of(interrupt.getAnswer()));
                    }
                    return item.withPlaces(List.of(interrupt.getAnswer()));
                }).collect(Collectors.toList());
            }

            String guide = guideService.getGuide(session.getUserId(), session.getReportType());
            String reportJson;

            if (session.getReportType() == ReportType.EMOTION) {
                String summaryJson = emotionProcessor.buildDiariesSummaryJson(items);
                String content = openAiReportClient.generateEmotionReportContent(summaryJson, guide);
                reportJson = objectMapper.writeValueAsString(
                    emotionProcessor.buildReport(session, items, content));
            } else {
                List<Diary> diaries = diaryRepository.findByUserIdAndDiaryDateBetweenOrderByDiaryDateAsc(
                    session.getUserId(), session.getPeriodStart(), session.getPeriodEnd());
                Map<Long, Diary> diaryMap = diaries.stream()
                    .collect(Collectors.toMap(Diary::getId, d -> d));
                Map<Long, String> diaryIdToEmotion = items.stream()
                    .filter(i -> i.emotions() != null && !i.emotions().isEmpty())
                    .collect(Collectors.toMap(DiaryAnalysisItem::diaryId,
                        i -> i.emotions().get(0), (a, b) -> a));
                String summaryJson = placeProcessor.buildDiariesSummaryJson(items, diaryIdToEmotion);
                String content = openAiReportClient.generatePlaceReportContent(summaryJson, guide);
                reportJson = objectMapper.writeValueAsString(
                    placeProcessor.buildReport(session, items, content, diaryMap));
            }

            session.setReportJson(reportJson);
            session.setStatus(ReportStatus.DONE);
            sessionRepository.save(session);
            log.info("performGeneration 완료 → DONE sessionId={}", session.getId());
        } catch (Exception e) {
            log.error("performGeneration 실패 sessionId={}: {}", session.getId(), e.getMessage(), e);
            session.setStatus(ReportStatus.ERROR);
            session.setErrorMessage("리포트 생성 중 오류가 발생했습니다.");
            sessionRepository.save(session);
        }
    }
}
