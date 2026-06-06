package com.plog.api.domain.report.service;

import com.plog.api.domain.aiguide.StateMemoryService;
import com.plog.api.domain.report.entity.ReportType;
import com.plog.api.llm.OpenAiReportClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportGuideService {

    private static final String EMOTION_GUIDE_KEY = "emotion_report_guide_md";
    private static final String PLACE_GUIDE_KEY = "place_report_guide_md";
    private static final int MAX_GUIDE_LINES = 20;

    private final StateMemoryService stateMemoryService;
    private final OpenAiReportClient openAiReportClient;

    public String getGuide(Long userId, ReportType type) {
        String key = type == ReportType.EMOTION ? EMOTION_GUIDE_KEY : PLACE_GUIDE_KEY;
        return stateMemoryService.getRawJson(userId, key).orElse(null);
    }

    public void appendFromFeedback(Long userId, int rating, String comment, String reportContent, ReportType type) {
        String key = type == ReportType.EMOTION ? EMOTION_GUIDE_KEY : PLACE_GUIDE_KEY;
        String existing = stateMemoryService.getRawJson(userId, key).orElse(null);
        try {
            String newGuide = openAiReportClient.refineReportGuide(existing, reportContent, comment, rating, MAX_GUIDE_LINES);
            stateMemoryService.upsert(userId, key, newGuide);
        } catch (Exception e) {
            log.warn("피드백 가이드 업데이트 실패 userId={} type={}: {}", userId, type, e.getMessage());
        }
    }
}
