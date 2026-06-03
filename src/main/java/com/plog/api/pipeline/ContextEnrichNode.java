package com.plog.api.pipeline;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.TextStyle;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriBuilder;

import com.fasterxml.jackson.databind.JsonNode;
import com.plog.api.pipeline.dto.ContextResult;
import com.plog.api.pipeline.dto.ExifResult;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * EXIF의 촬영 시간·GPS를 기반으로 일기 작성용 부가 컨텍스트 추정.
 * GPS가 있으면 Kakao Local API로 주소를, Open-Meteo API로 현재 날씨를 조회한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ContextEnrichNode {

    private final WebClient webClient;

    @Value("${plog.kakao.rest-api-key:}")
    private String kakaoRestApiKey;

    @Value("${plog.kakao.coord-to-address-url:https://dapi.kakao.com/v2/local/geo/coord2address.json}")
    private String kakaoCoordToAddressUrl;

    @Value("${plog.open-meteo.forecast-url:https://api.open-meteo.com/v1/forecast}")
    private String openMeteoForecastUrl;

    public ContextResult enrich(ExifResult exif) {
        if (exif == null) return ContextResult.empty();

        String fallbackLocation = locationMock(exif.latitude(), exif.longitude());
        String location = lookupKakaoAddress(exif.latitude(), exif.longitude(), fallbackLocation);
        WeatherInfo weather = lookupOpenMeteoWeather(exif.latitude(), exif.longitude());

        return ContextResult.builder()
                .season(seasonFrom(exif.capturedAt()))
                .dayOfWeek(dayOfWeekFrom(exif.capturedAt()))
                .holidayHint(holidayHintFrom(exif.capturedAt()))
                .weather(weather.weather())
                .temperature(weather.temperature())
                .locationHint(location)
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

    private String lookupKakaoAddress(Double lat, Double lon, String fallback) {
        if (lat == null || lon == null) return fallback;
        if (kakaoRestApiKey == null || kakaoRestApiKey.isBlank()) {
            log.debug("Kakao REST API key 미설정 → 위치 fallback 사용");
            return fallback;
        }

        try {
            JsonNode root = webClient.get()
                    .uri(kakaoCoordToAddressUrl, uriBuilder -> uriBuilder
                            .queryParam("x", lon)
                            .queryParam("y", lat)
                            .queryParam("input_coord", "WGS84")
                            .build())
                    .header("Authorization", "KakaoAK " + kakaoRestApiKey)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();

            JsonNode docs = root == null ? null : root.path("documents");
            if (docs == null || !docs.isArray() || docs.isEmpty()) return fallback;

            JsonNode first = docs.get(0);
            JsonNode address = first.path("address");
            if (address.isMissingNode() || address.isNull()) {
                address = first.path("road_address");
            }
            if (address.isMissingNode() || address.isNull()) return fallback;

            String region1 = address.path("region_1depth_name").asText("");
            String region2 = address.path("region_2depth_name").asText("");
            String region3 = address.path("region_3depth_name").asText("");
            String combined = String.join(" ", region1, region2, region3).trim().replaceAll("\\s+", " ");
            return combined.isBlank() ? fallback : combined;
        } catch (Exception e) {
            log.warn("Kakao 주소 조회 실패 lat={} lon={} err={}", lat, lon, e.getMessage());
            return fallback;
        }
    }

    private WeatherInfo lookupOpenMeteoWeather(Double lat, Double lon) {
        if (lat == null || lon == null) return WeatherInfo.unknown();

        try {
            JsonNode root = webClient.get()
                    .uri(openMeteoForecastUrl, uriBuilder -> openMeteoUri(uriBuilder, lat, lon))
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();

            JsonNode current = root == null ? null : root.path("current");
            if (current == null || current.isMissingNode() || current.isNull()) return WeatherInfo.unknown();

            Double temperature = current.path("temperature_2m").isNumber()
                    ? current.path("temperature_2m").asDouble()
                    : null;
            Integer weatherCode = current.path("weather_code").isNumber()
                    ? current.path("weather_code").asInt()
                    : null;
            return new WeatherInfo(toKoreanWeather(weatherCode, temperature), temperature);
        } catch (Exception e) {
            log.warn("Open-Meteo 날씨 조회 실패 lat={} lon={} err={}", lat, lon, e.getMessage());
            return WeatherInfo.unknown();
        }
    }

    private java.net.URI openMeteoUri(UriBuilder uriBuilder, Double lat, Double lon) {
        return uriBuilder
                .queryParam("latitude", lat)
                .queryParam("longitude", lon)
                .queryParam("current", "temperature_2m,weather_code")
                .queryParam("timezone", "Asia/Seoul")
                .build();
    }

    private String toKoreanWeather(Integer code, Double temperature) {
        String name = switch (code == null ? -1 : code) {
            case 0 -> "☀️ 맑음";
            case 1, 2 -> "🌤️ 대체로 맑음";
            case 3 -> "☁️ 흐림";
            case 45, 48 -> "🌫️ 안개";
            case 51, 53, 55, 56, 57 -> "🌦️ 이슬비";
            case 61, 63, 65, 66, 67, 80, 81, 82 -> "🌧️ 비";
            case 71, 73, 75, 77, 85, 86 -> "❄️ 눈";
            case 95, 96, 99 -> "⛈️ 천둥번개";
            default -> "날씨 정보";
        };
        if (temperature == null) return name;
        return name + " " + String.format(Locale.KOREAN, "%.0f℃", temperature);
    }
    /**
     * GPS 좌표를 한국 광역/지역 지명으로 변환.
     * Kakao API 실패/미설정 시 fallback으로 사용한다.
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
    private record WeatherInfo(String weather, Double temperature) {
        private static WeatherInfo unknown() {
            return new WeatherInfo("미상", null);
        }
    }
}
