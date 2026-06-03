package com.plog.api.llm;

import com.plog.api.pipeline.dto.DiaryAnalysisResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "plog.gemini", name = "use-mock", havingValue = "true", matchIfMissing = true)
public class OpenAiReportClientMock implements OpenAiReportClient {

    @Override
    public DiaryAnalysisResult analyzeEmotionFromDiary(String body, LocalDate date) {
        log.info("[Mock] analyzeEmotionFromDiary date={}", date);
        if (date != null && date.getDayOfMonth() % 5 == 0) {
            return new DiaryAnalysisResult(null, null, true,
                date + " 일기의 감정이 불명확합니다. 실제로 어떤 감정이었나요?",
                List.of("기쁨", "슬픔", "평온"));
        }
        if (date == null) return new DiaryAnalysisResult(List.of("평온"), null, false, null, null);
        // 날짜 기반으로 단일 또는 복수 감정 반환
        String[][] emotionSets = {{"기쁨"}, {"평온"}, {"설렘", "기쁨"}, {"슬픔"}, {"그리움", "우울"}};
        List<String> emotions = List.of(emotionSets[Math.abs(date.getDayOfMonth() % emotionSets.length)]);
        return new DiaryAnalysisResult(emotions, null, false, null, null);
    }

    @Override
    public DiaryAnalysisResult analyzePlaceFromDiary(String body, String location, LocalDate date) {
        log.info("[Mock] analyzePlaceFromDiary date={} location={}", date, location);
        if (location != null && !location.isBlank()) {
            return new DiaryAnalysisResult(null, List.of(location), false, null, null);
        }
        if (date != null && date.getDayOfMonth() % 7 == 0) {
            return new DiaryAnalysisResult(null, null, true,
                date + " 일기에서 방문한 주요 장소가 어디였나요?",
                List.of("카페", "공원", "집 근처"));
        }
        if (date == null) return new DiaryAnalysisResult(List.of("평온"), List.of("강남역"), false, null, null);
        String[] places = {"강남역", "홍대", "이태원", "한강공원", "카페거리"};
        String[] emotions = {"평온", "기쁨", "설렘", "그리움", "뿌듯함"};
        int idx = Math.abs(date.getDayOfMonth() % places.length);
        return new DiaryAnalysisResult(List.of(emotions[idx]), List.of(places[idx]), false, null, null);
    }

    @Override
    public String generateEmotionReportContent(String diariesSummaryJson, String guideMarkdown) {
        log.info("[Mock] generateEmotionReportContent");
        return "이번 주 당신은 기쁨과 평온함을 주로 느꼈어요. 일상 속 소소한 순간들에서 감정이 풍부하게 드러났답니다. " +
            "특히 중반부에는 설렘이 느껴지는 기록이 많았네요. ✨";
    }

    @Override
    public String generatePlaceReportContent(String diariesSummaryJson, String guideMarkdown) {
        log.info("[Mock] generatePlaceReportContent");
        return "이번 달 당신이 가장 자주 찾은 곳은 강남역 근처였어요. 그곳에서 주로 기쁨을 느꼈다는 게 기록에서 잘 드러나네요. " +
            "다양한 장소를 누비며 감정을 쌓아온 한 달이었답니다. 🗺️";
    }

    @Override
    public String refineReportGuide(String existingMd, String reportContent, String feedback, int rating, int maxLines) {
        log.info("[Mock] refineReportGuide rating={}", rating);
        StringBuilder sb = new StringBuilder("# 리포트 작성 취향\n");
        if (existingMd != null && !existingMd.isBlank()) {
            for (String line : existingMd.split("\\r?\\n")) {
                if (!line.isBlank() && !line.startsWith("# ")) sb.append(line).append('\n');
            }
        }
        if (feedback != null && !feedback.isBlank()) {
            sb.append("- (피드백 ").append(rating).append("점) ").append(feedback.trim()).append('\n');
        }
        return sb.toString();
    }
}
