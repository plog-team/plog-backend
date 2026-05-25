package com.plog.api.pipeline;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.TextStyle;
import java.util.Locale;

import org.springframework.stereotype.Component;

import com.plog.api.pipeline.dto.ContextResult;
import com.plog.api.pipeline.dto.ExifResult;

import lombok.extern.slf4j.Slf4j;

/**
 * EXIF의 촬영 시간·GPS를 기반으로 일기 작성용 부가 컨텍스트 추정.
 * 날씨는 현재 Mock (weather="미상") — OpenWeather 연동 시 교체.
 */
@Slf4j
@Component
public class ContextEnrichNode {

    public ContextResult enrich(ExifResult exif) {
        if (exif == null) return ContextResult.empty();
        return ContextResult.builder()
                .season(seasonFrom(exif.capturedAt()))
                .dayOfWeek(dayOfWeekFrom(exif.capturedAt()))
                .holidayHint(holidayHintFrom(exif.capturedAt()))
                .weather(weatherMock(exif.latitude(), exif.longitude()))
                .temperature(null) // OpenWeather 연동 시 채움
                .locationHint(locationMock(exif.latitude(), exif.longitude()))
                .build();
    }

    private String seasonFrom(LocalDateTime t) {
        if (t == null) return "미상";
        int m = t.getMonthValue();
        if (m >= 3 && m <= 5) return "봄";
        if (m >= 6 && m <= 8) return "여름";
        if (m >= 9 && m <= 11) return "가을";
        return "겨울";
    }

    private String dayOfWeekFrom(LocalDateTime t) {
        LocalDate d = (t == null) ? LocalDate.now() : t.toLocalDate();
        return d.getDayOfWeek().getDisplayName(TextStyle.NARROW, Locale.KOREAN); // 월/화/수/...
    }

    private String holidayHintFrom(LocalDateTime t) {
        LocalDate d = (t == null) ? LocalDate.now() : t.toLocalDate();
        DayOfWeek dw = d.getDayOfWeek();
        return (dw == DayOfWeek.SATURDAY || dw == DayOfWeek.SUNDAY) ? "주말" : "평일";
    }

    private String weatherMock(Double lat, Double lon) {
        // TODO: OpenWeather API 연동
        //   GET api.openweathermap.org/data/2.5/weather?lat={lat}&lon={lon}&appid=...&lang=kr
        if (lat == null || lon == null) return "미상";
        return "미상"; // Mock fallback
    }

    /**
     * GPS 좌표를 한국 광역/지역 지명으로 변환.
     * Gemini 캡션에 구체 지명을 공급해 모호한 표현을 방지하기 위함.
     */
    private String locationMock(Double lat, Double lon) {
        if (lat == null || lon == null) return null;
        // 제주도 (33.1~33.6N, 126.1~126.95E) — 시연 사진 영역, 세분화
        if (lat >= 33.1 && lat <= 33.6 && lon >= 126.1 && lon <= 126.95) {
            if (lat >= 33.45 && lon >= 126.55) return "제주 함덕·조천 인근";
            if (lat >= 33.40 && lon <= 126.30) return "제주 한림·협재 인근";
            if (lat >= 33.40 && lon >= 126.50) return "제주 한라산 북측";
            if (lat <= 33.30 && lon >= 126.50) return "제주 서귀포 인근";
            if (lat <= 33.30 && lon <= 126.30) return "제주 대정·모슬포 인근";
            return "제주도";
        }
        // 서울 (37.42~37.70N, 126.76~127.18E)
        if (lat >= 37.42 && lat <= 37.70 && lon >= 126.76 && lon <= 127.18) return "서울";
        // 부산 (35.05~35.35N, 128.95~129.30E)
        if (lat >= 35.05 && lat <= 35.35 && lon >= 128.95 && lon <= 129.30) return "부산";
        // 인천 (37.30~37.60N, 126.40~126.80E)
        if (lat >= 37.30 && lat <= 37.60 && lon >= 126.40 && lon <= 126.80) return "인천";
        // 경기 광역
        if (lat >= 37.10 && lat <= 38.20 && lon >= 126.50 && lon <= 127.80) return "경기 지역";
        // 강원 광역
        if (lat >= 37.10 && lat <= 38.60 && lon >= 127.50 && lon <= 129.40) return "강원 지역";
        // 한국 영역 광역 (위 매칭 안 되면)
        if (lat >= 33 && lat <= 38.7 && lon >= 124 && lon <= 132) {
            return String.format("한국 (위도 %.2f, 경도 %.2f)", lat, lon);
        }
        return String.format("위도 %.4f, 경도 %.4f", lat, lon);
    }
}
