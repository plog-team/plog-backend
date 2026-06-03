package com.plog.api.llm;

import com.plog.api.pipeline.dto.DiaryAnalysisResult;
import java.time.LocalDate;

public interface OpenAiReportClient {

    // 사용자가 감정 interrupt에서 "건너뛰기"를 선택했을 때의 고정 문자열
    String EMOTION_SKIP_ANSWER = "이 날은 기록하지 않을게요";

    DiaryAnalysisResult analyzeEmotionFromDiary(String body, LocalDate date);

    DiaryAnalysisResult analyzePlaceFromDiary(String body, String location, LocalDate date);

    String generateEmotionReportContent(String diariesSummaryJson, String guideMarkdown);

    String generatePlaceReportContent(String diariesSummaryJson, String guideMarkdown);

    String refineReportGuide(String existingMd, String reportContent, String feedback, int rating, int maxLines);
}
