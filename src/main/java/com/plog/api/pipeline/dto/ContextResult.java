package com.plog.api.pipeline.dto;

import lombok.Builder;

/**
 * ContextEnrichNode 출력. EXIF의 capturedAt/GPS를 기반으로 추정한 일기 작성 부가 컨텍스트.
 */
@Builder
public record ContextResult(
    String season,        // 봄/여름/가을/겨울
    String dayOfWeek,     // 월/화/수/목/금/토/일
    String holidayHint,   // 평일/주말 등
    String weather,       // OpenWeather 미연동 시 "미상"
    Double temperature,   // OpenWeather 미연동 시 null
    String locationHint   // 좌표 → 지역명 (Mock fallback: "위도 lat, 경도 lon")
) {
    public static ContextResult empty() {
        return ContextResult.builder().season("미상").dayOfWeek("미상").build();
    }
}
