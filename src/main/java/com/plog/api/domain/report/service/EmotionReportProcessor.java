package com.plog.api.domain.report.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.plog.api.domain.diary.Diary;
import com.plog.api.domain.report.dto.response.EmotionByDay;
import com.plog.api.domain.report.dto.response.EmotionReportData;
import com.plog.api.domain.report.entity.ReportSession;
import com.plog.api.llm.OpenAiReportClient;
import com.plog.api.pipeline.dto.DiaryAnalysisItem;
import com.plog.api.pipeline.dto.DiaryAnalysisResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmotionReportProcessor {

    private static final String[] DAYS_KR = {"월", "화", "수", "목", "금", "토", "일"};

    private final OpenAiReportClient openAiReportClient;
    private final ObjectMapper objectMapper;

    public List<DiaryAnalysisResult> analyzeForInterrupts(List<Diary> diaries) {
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<CompletableFuture<DiaryAnalysisResult>> futures = diaries.stream()
                .map(diary -> CompletableFuture.supplyAsync(() -> {
                    try {
                        return openAiReportClient.analyzeEmotionFromDiary(diary.getBody(), diary.getDiaryDate());
                    } catch (Exception e) {
                        log.warn("감정 분석 실패 diaryId={}: {}", diary.getId(), e.getMessage());
                        return new DiaryAnalysisResult(null, null, false, null, null);
                    }
                }, executor))
                .collect(Collectors.toList());
            return futures.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toList());
        }
    }

    public String buildDiariesSummaryJson(List<DiaryAnalysisItem> items) {
        List<Map<String, Object>> list = items.stream().map(item -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("date", item.dateStr());
            map.put("day", dayOfWeek(LocalDate.parse(item.dateStr())));
            map.put("emotions", item.emotions() != null ? item.emotions() : Collections.emptyList());
            if (item.bodyExcerpt() != null && !item.bodyExcerpt().isBlank()) {
                map.put("excerpt", item.bodyExcerpt());
            }
            return map;
        }).collect(Collectors.toList());
        try {
            return objectMapper.writeValueAsString(list);
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }

    public EmotionReportData buildReport(ReportSession session, List<DiaryAnalysisItem> items, String content) {
        String period = String.format("%d월 %d일 - %d월 %d일",
            session.getPeriodStart().getMonthValue(), session.getPeriodStart().getDayOfMonth(),
            session.getPeriodEnd().getMonthValue(), session.getPeriodEnd().getDayOfMonth());

        Map<String, Integer> frequency = new LinkedHashMap<>();
        List<EmotionByDay> byDay = new ArrayList<>();

        Set<String> diaryDates = items.stream()
            .map(DiaryAnalysisItem::dateStr)
            .collect(Collectors.toSet());

        Map<String, List<String>> dateToEmotions = items.stream()
            .filter(i -> i.emotions() != null && !i.emotions().isEmpty())
            .collect(Collectors.toMap(DiaryAnalysisItem::dateStr, DiaryAnalysisItem::emotions, (a, b) -> a));

        items.stream()
            .filter(i -> i.emotions() != null)
            .flatMap(i -> i.emotions().stream())
            .filter(Objects::nonNull)
            .forEach(e -> frequency.merge(e, 1, Integer::sum));

        LocalDate cur = session.getPeriodStart();
        while (!cur.isAfter(session.getPeriodEnd())) {
            String dateStr = cur.toString();
            byDay.add(new EmotionByDay(dateStr, dayOfWeek(cur),
                dateToEmotions.get(dateStr), diaryDates.contains(dateStr)));
            cur = cur.plusDays(1);
        }

        String primaryEmotion = frequency.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse(null);

        return new EmotionReportData(period, content, primaryEmotion, frequency, byDay);
    }

    private String dayOfWeek(LocalDate date) {
        return DAYS_KR[date.getDayOfWeek().getValue() - 1];
    }
}
