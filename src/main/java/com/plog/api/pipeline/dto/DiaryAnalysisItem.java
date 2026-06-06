package com.plog.api.pipeline.dto;

import java.util.List;

public record DiaryAnalysisItem(
    Long diaryId,
    String dateStr,
    List<String> emotions,
    List<String> places,
    String bodyExcerpt
) {
    public DiaryAnalysisItem withEmotions(List<String> newEmotions) {
        return new DiaryAnalysisItem(diaryId, dateStr, newEmotions, places, bodyExcerpt);
    }

    public DiaryAnalysisItem withPlaces(List<String> newPlaces) {
        return new DiaryAnalysisItem(diaryId, dateStr, emotions, newPlaces, bodyExcerpt);
    }
}
