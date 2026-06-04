package com.plog.api.domain.photo;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.databind.JsonNode;
import com.plog.api.pipeline.dto.ContextResult;
import com.plog.api.pipeline.dto.ExifResult;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import jakarta.annotation.PostConstruct;
/**
 * 사진 EXIF의 GPS와 촬영 시각을 Kakao Local 및 Open-Meteo 결과로 보강한다.
 * 외부 API 실패는 사진 업로드를 실패시키지 않고 해당 필드만 비워 둔다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PhotoAutoInputEnricher {

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    private static final Duration API_TIMEOUT = Duration.ofSeconds(5);

    private final WebClient webClient;

    @Value("${plog.autofill.kakao.api-key:}")
    private String kakaoApiKey;
    // 서버 시작 시 카카오 API 키 확인
    @PostConstruct
    public void checkApiKey() {
        System.out.println("KAKAO API KEY = " + kakaoApiKey);
    }

    @Value("${plog.autofill.kakao.coord-to-address-url:https://dapi.kakao.com/v2/local/geo/coord2address.json}")
    private String kakaoCoordToAddressUrl;

    @Value("${plog.autofill.open-meteo.forecast-url:https://api.open-meteo.com/v1/forecast}")
    private String openMeteoForecastUrl;

    @Value("${plog.autofill.open-meteo.archive-url:https://archive-api.open-meteo.com/v1/archive}")
    private String openMeteoArchiveUrl;

    public ContextResult enrich(ExifResult exif) {
        if (exif == null || exif.latitude() == null || exif.longitude() == null) {
            return ContextResult.empty();
        }

        String location = fetchLocation(exif.latitude(), exif.longitude());
        WeatherSnapshot weather = fetchWeather(exif.latitude(), exif.longitude(), exif.capturedAt());

        return ContextResult.builder()
                .locationHint(location)
                .weather(weather == null ? null : weather.description())
                .temperature(weather == null ? null : weather.temperature())
                .build();
    }

    private String fetchLocation(double latitude, double longitude) {
        if (kakaoApiKey == null || kakaoApiKey.isBlank()) {
            log.debug("Kakao REST API key가 없어 사진 위치 자동입력을 생략합니다");
            return null;
        }

        String uri = UriComponentsBuilder.fromUriString(kakaoCoordToAddressUrl)
                .queryParam("x", longitude)
                .queryParam("y", latitude)
                .queryParam("input_coord", "WGS84")
                .build(true)
                .toUriString();
        try {
            JsonNode response = webClient.get()
                    .uri(uri)
                    .header(HttpHeaders.AUTHORIZATION, "KakaoAK " + kakaoApiKey)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block(API_TIMEOUT);
            return parseKakaoAddress(response);
        } catch (Exception e) {
            log.warn("Kakao 주소 조회 실패: {}", e.getMessage());
            return null;
        }
    }

    private WeatherSnapshot fetchWeather(double latitude, double longitude, LocalDateTime capturedAt) {
        if (capturedAt == null) return null;

        LocalDate date = capturedAt.toLocalDate();
        String baseUrl = date.isBefore(LocalDate.now(SEOUL)) ? openMeteoArchiveUrl : openMeteoForecastUrl;
        String uri = UriComponentsBuilder.fromUriString(baseUrl)
                .queryParam("latitude", latitude)
                .queryParam("longitude", longitude)
                .queryParam("start_date", date)
                .queryParam("end_date", date)
                .queryParam("hourly", "temperature_2m,weather_code")
                .queryParam("timezone", "Asia/Seoul")
                .build(true)
                .toUriString();
        try {
            JsonNode response = webClient.get()
                    .uri(uri)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block(API_TIMEOUT);
            return parseClosestWeather(response, capturedAt);
        } catch (Exception e) {
            log.warn("Open-Meteo 날씨 조회 실패: {}", e.getMessage());
            return null;
        }
    }

    String parseKakaoAddress(JsonNode response) {
        JsonNode documents = response == null ? null : response.path("documents");
        if (documents == null || !documents.isArray() || documents.isEmpty()) return null;

        JsonNode first = documents.get(0);
        String address = textOrNull(first.path("address"), "address_name");
        return address != null ? address : textOrNull(first.path("road_address"), "address_name");
    }

    WeatherSnapshot parseClosestWeather(JsonNode response, LocalDateTime capturedAt) {
        if (response == null || capturedAt == null) return null;
        JsonNode hourly = response.path("hourly");
        JsonNode times = hourly.path("time");
        JsonNode temperatures = hourly.path("temperature_2m");
        JsonNode weatherCodes = hourly.path("weather_code");
        int size = Math.min(times.size(), Math.min(temperatures.size(), weatherCodes.size()));
        if (size == 0) return null;

        int closestIndex = 0;
        long closestMinutes = Long.MAX_VALUE;
        for (int i = 0; i < size; i++) {
            LocalDateTime apiTime = LocalDateTime.parse(times.get(i).asText());
            long minutes = Math.abs(Duration.between(capturedAt, apiTime).toMinutes());
            if (minutes < closestMinutes) {
                closestMinutes = minutes;
                closestIndex = i;
            }
        }

        JsonNode temperatureNode = temperatures.get(closestIndex);
        JsonNode weatherCodeNode = weatherCodes.get(closestIndex);
        if (!temperatureNode.isNumber() || !weatherCodeNode.isInt()) return null;

        double temperature = temperatureNode.asDouble();
        int weatherCode = weatherCodeNode.asInt();
        return new WeatherSnapshot(weatherDescription(weatherCode), temperature);
    }

    private String textOrNull(JsonNode parent, String field) {
        if (parent == null || parent.isMissingNode() || parent.isNull()) return null;
        String value = parent.path(field).asText(null);
        return value == null || value.isBlank() ? null : value;
    }

    private String weatherDescription(int code) {
        if (code == 0) return "맑음";
        if (List.of(1, 2, 3).contains(code)) return "흐림";
        if (List.of(45, 48).contains(code)) return "안개";
        if (List.of(51, 53, 55, 56, 57).contains(code)) return "이슬비";
        if (List.of(61, 63, 65, 66, 67, 80, 81, 82).contains(code)) return "비";
        if (List.of(71, 73, 75, 77, 85, 86).contains(code)) return "눈";
        if (List.of(95, 96, 99).contains(code)) return "천둥번개";
        return "미상";
    }

    record WeatherSnapshot(String description, Double temperature) {}
}