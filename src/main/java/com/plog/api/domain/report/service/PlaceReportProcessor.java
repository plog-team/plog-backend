package com.plog.api.domain.report.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.plog.api.domain.diary.Diary;
import com.plog.api.domain.report.dto.response.PlaceEntry;
import com.plog.api.domain.report.dto.response.PlaceReportData;
import com.plog.api.domain.report.entity.ReportSession;
import com.plog.api.llm.OpenAiReportClient;
import com.plog.api.pipeline.dto.DiaryAnalysisItem;
import com.plog.api.pipeline.dto.DiaryAnalysisResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlaceReportProcessor {

    private final OpenAiReportClient openAiReportClient;
    private final ObjectMapper objectMapper;

    public List<DiaryAnalysisResult> analyzeForInterrupts(List<Diary> diaries) {
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<CompletableFuture<DiaryAnalysisResult>> futures = diaries.stream()
                .map(diary -> CompletableFuture.supplyAsync(() -> {
                    try {
                        return openAiReportClient.analyzePlaceFromDiary(
                            diary.getBody(), diary.getLocation(), diary.getDiaryDate());
                    } catch (Exception e) {
                        log.warn("장소 분석 실패 diaryId={}: {}", diary.getId(), e.getMessage());
                        return new DiaryAnalysisResult(null, null, false, null, null);
                    }
                }, executor))
                .collect(Collectors.toList());
            return futures.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toList());
        }
    }

    public String buildDiariesSummaryJson(List<DiaryAnalysisItem> items, Map<Long, String> diaryIdToEmotion) {
        // place → 해당 일기 items 목록 (한 일기가 여러 장소에 중복 등장 가능)
        Map<String, List<DiaryAnalysisItem>> placeToItems = new LinkedHashMap<>();
        for (DiaryAnalysisItem item : items) {
            if (item.places() == null || item.places().isEmpty()) continue;
            for (String place : item.places()) {
                placeToItems.computeIfAbsent(place, k -> new ArrayList<>()).add(item);
            }
        }
        List<Map<String, Object>> list = placeToItems.entrySet().stream()
            .map(e -> {
                List<String> emotions = e.getValue().stream()
                    .map(i -> diaryIdToEmotion.getOrDefault(i.diaryId(), "기록 없음"))
                    .collect(Collectors.toList());
                List<String> excerpts = e.getValue().stream()
                    .map(DiaryAnalysisItem::bodyExcerpt)
                    .filter(Objects::nonNull)
                    .filter(s -> !s.isBlank())
                    .collect(Collectors.toList());
                Map<String, Object> map = new LinkedHashMap<>();
                map.put("place", e.getKey());
                map.put("visits", e.getValue().size());
                map.put("emotions", emotions);
                if (!excerpts.isEmpty()) map.put("diary_excerpts", excerpts);
                return map;
            })
            .sorted((a, b) -> Integer.compare((int) b.get("visits"), (int) a.get("visits")))
            .collect(Collectors.toList());
        try {
            return objectMapper.writeValueAsString(list);
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }

    public PlaceReportData buildReport(ReportSession session, List<DiaryAnalysisItem> items,
                                       String content, Map<Long, Diary> diaryMap) {
        String period = String.format("%d년 %d월",
            session.getPeriodStart().getYear(), session.getPeriodStart().getMonthValue());

        // place → 해당 일기 items 목록
        Map<String, List<DiaryAnalysisItem>> byPlace = new LinkedHashMap<>();
        for (DiaryAnalysisItem item : items) {
            if (item.places() == null || item.places().isEmpty()) continue;
            for (String place : item.places()) {
                byPlace.computeIfAbsent(place, k -> new ArrayList<>()).add(item);
            }
        }

        List<PlaceEntry> places = byPlace.entrySet().stream()
            .sorted((a, b) -> Integer.compare(b.getValue().size(), a.getValue().size()))
            .map(e -> {
                String mainEmotion = e.getValue().stream()
                    .filter(i -> i.emotions() != null && !i.emotions().isEmpty())
                    .map(i -> i.emotions().get(0))
                    .collect(Collectors.groupingBy(em -> em, Collectors.counting()))
                    .entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse(null);
                String photoUrl = resolvePhotoUrl(e.getValue(), diaryMap);
                return new PlaceEntry(e.getKey(), e.getValue().size(), mainEmotion, photoUrl);
            })
            .collect(Collectors.toList());

        String topPhotoUrl = places.stream()
            .map(PlaceEntry::photoUrl)
            .filter(Objects::nonNull)
            .findFirst()
            .orElse(null);

        return new PlaceReportData(period, content, topPhotoUrl, places);
    }

    private String resolvePhotoUrl(List<DiaryAnalysisItem> items, Map<Long, Diary> diaryMap) {
        for (DiaryAnalysisItem item : items) {
            Diary diary = diaryMap.get(item.diaryId());
            if (diary == null || diary.getPhotoIdsCsv() == null || diary.getPhotoIdsCsv().isBlank()) continue;
            String[] ids = diary.getPhotoIdsCsv().split(",");
            int idx = Math.min(Math.max(diary.getRepresentativePhotoIndex(), 0), ids.length - 1);
            try {
                long photoId = Long.parseLong(ids[idx].trim());
                return "/api/photos/" + photoId;
            } catch (NumberFormatException ignored) {}
        }
        return null;
    }
}
