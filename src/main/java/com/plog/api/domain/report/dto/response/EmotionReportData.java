package com.plog.api.domain.report.dto.response;

import java.util.List;
import java.util.Map;

public record EmotionReportData(
    String period,
    String content,
    String primaryEmotion,
    Map<String, Integer> emotionFrequency,
    List<EmotionByDay> emotionByDay
) {}
